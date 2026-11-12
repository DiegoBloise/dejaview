# ADR-0015: Component interaction model

- **Status:** Accepted
- **Date:** 2026-11-12

## Context

ADR-0014 delivered the component contract: one `Component` instance per mounted
`ComponentRenderNode`, identity positional, cleanup through
`ResourceRegistry`, `close()` final in `AbstractComponent`. It deferred the
Sprint 08 surface — inputs, outputs, slots, lifecycle callbacks, keyed
reconciliation and dynamic component swaps.

Sprint 08 had to answer six questions without making `dejaview-core` depend on
the DOM or reactivity:

1. How are input values delivered, and when are they refreshed?
2. How does a parent listen to a child, and what happens on misuse?
3. How does projected content reach the child's tree?
4. Which lifecycle hooks exist, and in what order do they run?
5. When do sibling lists switch to keyed reconciliation?
6. When must a component node be replaced instead of reused?

## Decision

### Inputs are raw values, re-bound on every render pass

`ComponentView#input(String, Object)` captures values when the view is built.
The renderer pushes a fresh `ComponentBindings` into the mounted component
before every `render()` call. Child inputs therefore refresh as a consequence
of the parent re-rendering, not because the child tracks the values itself.

`AbstractComponent#input(String, Class)` validates type and presence and fails
fast with `DejaViewException` codes `missing-input` / `invalid-input`.
`inputOr(String, Class, T)` applies the same type check with a default.
Null input values are rejected at the view; blank names are rejected at both
levels.

Consequences:

- Inputs stay signal-agnostic. A parent may pass a plain value, a value read
  from a signal inside its own `render()`, or a constant; the child cannot
  tell the difference.
- Inputs are *not* re-evaluated reactively inside the child. The child
  re-renders only when its parent reconciles it (see ADR-0013 for the single
  effect boundary per mounted tree).
- Because raw values are captured at view construction, a view held across
  renders carries stale values; views are values and are meant to be rebuilt,
  not mutated.

### Outputs are consumers registered on the view

`ComponentView#on(String, Consumer<?>)` registers a listener; multiple
listeners per output run in registration order.
`AbstractComponent#emit(String, T)` invokes them synchronously. An output
with no listeners is a no-op. Listener exceptions propagate — there is no
swallowed error path and no `onError` hook yet.

No `EventEmitter` class is introduced: the view already carries registration
order and disposal (the bindings are rebuilt per pass, and stale listeners
simply are not re-registered).

### Projection is a direct splice, not a slot element

Slots are views attached to the component view (`slot(View...)` for the
default slot, `slot(String, View...)` for named slots). The component reads
them with `slot()` / `slot(String)` and places them anywhere in its rendered
tree. There is no `SlotView`, no wrapper element and no placeholder node:
projected content participates in reconciliation exactly like any other
views the component renders.

This keeps projected content's binding lifetime, reconciliation identity and
key handling identical to owned content, at the cost of projecting the same
views into two slots producing two mounted instances (same caveat as any
view reused twice in a tree).

### Lifecycle hooks are explicit and non-overridable boundaries

`Component` gains public default no-op hooks; `AbstractComponent` keeps
`close()` final and calls the documented sequence:

- mount: `bind(bindings)` → `onInit()` → `render()`
- re-render: `bind(bindings)` → `render()` → `onUpdate()`
- unmount: `onDestroy()` → `close()` → subtree dispose

`onDestroy()` runs before resources are released so cleanup code can rely on
`own(...)`ed resources still being open. `onUpdate()` does not run on the
initial mount — `onInit()` covers that case — and `onUpdate()` observes the
tree *after* reconciliation.

`onMount()` and `onError()` do not exist. Render errors propagate to the
single `Effect` that drives the mounted tree; an error boundary is a separate
design question.

### Keyed reconciliation is opt-in per list and fails fast on duplicates

Children lists reconcile by position unless at least one child in the *new*
list carries a key.

Keyed mode performs three passes:

1. Match new children to current nodes (keyed↔keyed by key; unkeyed↔keyed
   siblings by relative order), reconcile in place or build and append.
2. Detach and dispose unmatched current nodes.
3. Re-anchor DOM order only when the matched sequence is not increasing, a
   node was replaced, or a new child appears before the last matched child.
   In-place updates that keep order never move nodes, which preserves focus
   and selection.

Duplicate keys in the new list throw `DejaViewException("duplicate-key")`
before any DOM node is moved, so a failed render leaves the previous DOM
untouched.

`RenderNode` carries the view's key; an empty key is rejected at the view.

### Component identity across renders needs an explicit signal — no probe

Reusing the mounted instance is the default. Replacing it must be requested.
Two mechanisms are supported:

- distinct `key(...)` on the views (dynamic swap at one position); or
- an explicit class token on the view —
  `Views.componentFactory(Class, Supplier)` —
  which the reconciler compares with the token stored on the node. Both must
  be present and differ for replacement; otherwise the instance is reused.

An earlier design created a short-lived probe instance of the next factory's
component class on every render pass to compare classes. It was rejected:
a probe's constructor-registered `onDispose`/`own` side effects fire on every
render pass (spurious registrations on a throwaway instance), factories that
allocate would pay a hidden allocation per render, and a throwing factory
would fail the render before any reconciliation. The class token is explicit,
allocation-free, and ignoring it (tokenless views) degrades to positional
reuse — the pre-Sprint-08 behaviour — instead of failing at runtime.

Unkeyed dynamic swaps of *tokenless* views at the same position still reuse
positionally. Distinct keys or class tokens are the supported mechanism.

## Consequences

- The Sprint 08 roadmap is implementable without annotations, and the
  annotation API (`@Input`/`@Output`) remains a proposal layered on this
  programmatic surface.
- Re-render propagation flows from the single reactive boundary: parent
  re-renders, children re-bind, instances persist.
- Keyed lists preserve component and DOM node identity across reorders with
  no O(n²) pass beyond the key lookup and the conditional re-anchor.
- Fail-fast validation (`missing-input`, `invalid-input`, `duplicate-key`)
  surfaces configuration errors at the boundary instead of inside render.
- Duplicate-key failure is total: no partial DOM mutation happens.
- Projected content has no host element, so consumers of ARIA/selectors that
  assume wrappers must be adjusted to the spliced tree.
- `Component` now has public methods beyond `render()`/`close()`; lambdas
  still compile unchanged because the hooks are `default` no-ops.
- Debugging token mismatches requires the developer to keep tokens in sync
  with the factory; the type system cannot enforce it. A `componentFactory`
  that infers the token from the instance is possible future sugar but is
  not in this sprint.

## Alternatives considered

- **`Supplier` inputs evaluated per render inside the child.** Rejected:
  couples the child's read timing to parent render, hides re-render
  frequency, and forces every input to be a closure allocation.
- **`EventEmitter<T>` class with subscription lifecycle.** Rejected: the
  registration order and per-pass rebind already provide the semantics;
  an extra object per output adds state with no capability.
- **A `SlotView` element rendered as a placeholder.** Rejected: introduces a
  meaningless DOM node, extra reconciliation cases and divergent semantics
  for projected vs owned content.
- **Positional-only identity with keyed opt-in (Sprint 07 behaviour, kept as
  default).** Kept as default, but extended with keyed matching and class
  tokens; dynamic swapping now needs an explicit marker instead of being
  silently wrong.
- **Probe instance for class detection.** Rejected as described above.
- **Global `onError()` hook.** Deferred: error boundaries need a design of
  their own (recovery semantics, disposal of subtrees).

## Revisit

When the annotation API is specified, when a second renderer needs class-token
semantics (e.g. SSR), when error boundaries are introduced, or when keyed
lists are needed across fragment boundaries in ways the per-list reconciler
cannot express.
