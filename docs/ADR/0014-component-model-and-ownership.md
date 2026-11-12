# ADR-0014: Component model and ownership

- **Status:** Accepted
- **Date:** 2026-11-12

## Context

Sprint 05 introduced an explicit render tree and `ComponentView` as a lazy view
node. Sprint 06 used that node as the reactive boundary: one effect per mounted
tree re-resolved the view supplier and reconciled the DOM positionally.

That left a gap. A `ComponentView` was only a supplier of views:

- it had no instance that could hold component state;
- it had no identity, name or disposal hook;
- nothing guaranteed that a component instance survived re-renders;
- resources created by a component had no owner.

Sprint 07 must turn this into the component abstraction applications build on,
without making `dejaview-core` depend on the DOM or the reactive engine.

## Decision

### `Component` is a renderable, closeable unit

`dejaview-core` defines:

```java
@FunctionalInterface
public interface Component extends AutoCloseable {
    View render();
    default String name() { ... }
    default void close() { }
}
```

- `render()` returns the current view. It may read reactive state.
- `name()` is the component metadata used in diagnostics. The default is the
  simple class name; lambda components report `Component`.
- `close()` releases component-owned resources. The default is a no-op.

`Component` is a functional interface, so a stateless component is a lambda
returning a view. Class-based components override `render()` and keep fields as
component state.

### One instance per mounted node

`ComponentView` holds a factory, not a rendered view:

```java
public record ComponentView(Supplier<? extends Component> factory) implements View
```

`RenderTreeBuilder` calls `factory.get()` exactly once per mounted component
node and stores the instance in `ComponentRenderNode`. Re-renders call
`render()` on that same instance. Component state therefore lives as long as the
node, not as long as a render pass.

`ComponentRenderNode.dispose()` closes the component before disposing the
rendered subtree, so listeners and other resources registered by the component
are released together with its DOM.

### Composition APIs

`Views` exposes two entry points:

- `Views.component(Supplier<View>)` — stateless functional component. The
  supplier is wrapped in an internal `FunctionalComponent` whose `name()` is
  `"Function"`.
- `Views.componentFactory(Supplier<? extends Component>)` — creates one
  class-based component per mounted node.

`ComponentView.of(Component)` mounts an existing instance. It is intentionally
not exposed as `Views.component(Component)`: overloading `component` with both
`Supplier<View>` and `Component` makes lambda arguments ambiguous, because both
are functional interfaces. `of` keeps the call sites unambiguous.

### Resource ownership

`AbstractComponent` owns a `ResourceRegistry`:

```java
protected final <T extends AutoCloseable> T own(T resource)
protected final void onDispose(Runnable action)
@Override public final void close()
```

Resources are released in reverse registration order, disposal is idempotent,
and registering after disposal fails. `close()` is final so subclasses cannot
bypass the registry; cleanup is registered through `onDispose`.

The core stays independent of the reactive engine. An effect or computed created
by a component is registered with `own(...)`; core only sees `AutoCloseable`.

### Identity and lifecycle scope

Component identity is positional, matching the reconciler's existing model. If a
new view at the same position is another `ComponentView`, the mounted
`ComponentRenderNode` keeps its instance and calls `render()` again. Replacing
the node with a different node kind disposes the component.

Inputs, outputs, explicit lifecycle callbacks, keyed identity and dynamic
component swapping are Sprint 08 work. Until then, a component receives its
dependencies through its constructor or closure and communicates through signals.

### Naming and conventions

- Class-based components are named `*Component` and extend `AbstractComponent`
  when they own resources.
- Components return views; they do not touch the DOM.
- State belongs to the component instance or to a signal it owns.
- A component factory must return a new instance per call when the component is
  stateful. `ComponentView.of(instance)` is for deliberate single-instance
  mounting.

## Consequences

- Components can hold state and resources across re-renders without leaking
  them into the application or the renderer.
- The renderer creates and closes component instances; application code does not
  manage their lifecycle.
- Functional components remain concise for stateless markup.
- Positional identity means a component instance survives re-renders at the same
  position even when the rendered element type changes, and is recreated when its
  position is replaced. Keyed and typed identity remain future work.
- `close()` is final in `AbstractComponent`, so cleanup must be registered rather
  than overridden. This keeps disposal ordering deterministic.
- Effects created inside `render()` on every call are still a hazard; components
  must create them once (typically in the constructor or a lazy field) and
  register them with `own(...)`.

## Alternatives considered

- **`ComponentView(Supplier<View>)` plus a separate component registry.**
  Rejected: it splits identity and state across two structures without adding
  capability.
- **`Views.component(Component)` overload.** Rejected because lambda arguments
  become ambiguous with `Views.component(Supplier<View>)`. `ComponentView.of`
  covers the same need.
- **Components as plain view suppliers with no disposal.** Rejected: it leaves
  owned effects and listeners with no lifetime.
- **Overriding `close()` in `AbstractComponent` subclasses.** Rejected: it allows
  skipping the registry and makes disposal order unpredictable.
- **Dynamic component type swapping in Sprint 07.** Deferred: it needs a
  decision about identity and state migration, which belongs with inputs and
  lifecycle in Sprint 08.

## Revisit

When Sprint 08 adds inputs, outputs and lifecycle callbacks; when keyed or typed
component identity is required; or when a second renderer needs to create
components outside `dejaview-dom`.
