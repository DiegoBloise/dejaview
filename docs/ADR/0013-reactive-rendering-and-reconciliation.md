# ADR-0013: Reactive rendering and positional reconciliation

- **Status:** Accepted
- **Date:** 2026-11-12

## Context

Sprint 06 connects the renderer to the reactive system. Before it, `DomRenderer`
only performed a first mount and the counter example updated text with
`Dom.byId("count").setText(...)` inside an effect. That approach bypassed the
renderer, required stable ids and gave application code direct DOM access.

The renderer needs to:

- update text, attributes, properties, class/style attributes and children;
- remove and replace nodes;
- preserve unrelated DOM nodes;
- coalesce updates when several signals change together;
- release effects and listeners with the mounted tree.

`dejaview-core` must stay free of DOM and browser APIs. `dejaview-reactivity`
already depends on core and remains platform-independent.

## Decision

### Component views are the reactive boundary

A `ComponentView` is rendered by calling its supplier. The mounted tree owns one
`Effect`; the effect renders the view and therefore tracks every signal or
computed read while the component is resolved. When a dependency changes, the
effect reruns and reconciles the result.

`dejaview-dom` now depends on `dejaview-reactivity`:

```text
dejaview-dom → dejaview-reactivity → dejaview-core
```

The dependency is acyclic. Reactivity does not know about the DOM.

### Positional reconciliation

`DomTreeReconciler` matches render nodes by position:

- text nodes update their existing `DomText`;
- elements with the same tag update attributes, properties, events and children
  in place;
- a different node kind or tag name replaces the node, inserting the replacement
  at the old position and disposing the old subtree;
- extra children at the end are detached and disposed;
- new children at the end are appended.

There are no keys, move detection or fine-grained binding nodes yet. Those are
deferred until list rendering or a measured performance need justifies them.

### Scheduling

One effect per mounted tree is the update scheduler. Updates are synchronous
outside a batch. Inside `Reactive.batch(...)`, the effect runs once and the tree
is reconciled once. This reuses the existing reactive scheduler instead of
introducing a second scheduling mechanism.

### DOM contract additions

- `DomText.setText(String)` for in-place text updates.
- `DomElement.removeAttribute(String)` for attributes removed by a new view.
- `DomElement.setProperty(String, String)` and `removeProperty(String)` for DOM
  properties.
- `DomElement.insertBefore(DomNode, DomNode)` so replacements preserve order.

`Property` was added to the core view model. Class and style updates remain
attributes (`class` and `style`), matching the browser API.

### Example

`CounterApp` mounts `Views.component(this::view)` and only changes a signal.
`Dom.byId` is no longer used by application code.

## Consequences

- A signal update reaches the DOM through the renderer; application code no
  longer needs element ids or direct DOM access.
- Unrelated nodes keep their identity, so focus, selection and listeners survive
  updates that do not touch them.
- Updates are component-scoped: a signal read while rendering reruns the
  component and reconciles its subtree. This is coarser than per-node bindings
  but simpler and sufficient for the current model.
- Positional matching can recreate nodes when children are reordered. Keyed
  reconciliation is future work.
- `dejaview-dom` now pulls in the reactive engine. A renderer without reactivity
  is no longer possible, which is acceptable because reactive updates are part
  of the framework's core value.
- Event handlers are compared by equality. Re-rendering with new handler
  instances removes and re-registers the listeners; listener count does not grow.

## Alternatives considered

- **Fine-grained `BoundText`/`BoundAttribute` nodes.** Deferred: they duplicate
  what component re-rendering already provides and would be premature before the
  component model in Sprint 07.
- **Keep the renderer independent of reactivity.** Rejected: it would require a
  second observation mechanism or force application code to keep using
  `Dom.byId`.
- **Microtask-based scheduling.** Deferred: the existing synchronous effect
  scheduler already provides coalescing through `Reactive.batch`, and a browser
  microtask adapter would add a platform-specific scheduler.
- **Keyed reconciliation now.** Deferred until list rendering and its identity
  requirements are specified.

## Revisit

When the component model introduces owned scopes (Sprint 07), when list
rendering requires keys, or when a second rendering backend needs a
platform-neutral update pipeline.
