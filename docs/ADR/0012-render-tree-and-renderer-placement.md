# ADR-0012: Render tree and renderer placement

- **Status:** Accepted
- **Date:** 2026-11-12

## Context

Sprint 05 required an explicit render tree, component nodes, renderer contracts
and a renderer lifecycle. Two placements were possible:

1. put the mounted render tree in `dejaview-core`, as the long-term
   architecture sketch suggests; or
2. keep the immutable view model in `dejaview-core` and put the mounted tree in
   `dejaview-dom`.

The mounted tree owns concrete `DomNode` instances, event registrations and
DOM-shaped children. Moving it to core would force core to know about DOM
contracts, violating ADR-0003.

The roadmap also lists `dejaview-renderer` as a Sprint 05 deliverable, while
ADR-0003 says a separate renderer module is introduced only when a second
backend exists. Sprint 05 has one backend.

## Decision

- The immutable `View` model stays in `dejaview-core`. `ComponentView` is a
  core node whose supplier is resolved by a renderer, so core does not depend on
  DOM types.
- The mounted render tree (`RenderNode`, `TextRenderNode`,
  `ElementRenderNode`, `FragmentRenderNode`, `ComponentRenderNode`) lives in
  `dejaview-dom`, because it owns DOM nodes, listeners and disposal.
- `Renderer` defines the mount contract and `MountedDom` owns the mounted tree
  lifecycle.
- `TreeReconciler` defines the in-place update contract. Sprint 05 only defines
  it; Sprint 06 implements it.
- No `dejaview-renderer` module is created yet. The renderer remains in
  `dejaview-dom` per ADR-0003.

## Consequences

- Core remains free of browser and DOM APIs.
- The render tree can dispose listeners and child nodes without re-walking the
  original view.
- `DejaViewRuntime` accepts a `Renderer`, so tests and future backends can
  supply an implementation without changing the runtime.
- The architecture sketch still describes a platform-neutral render tree as a
  long-term goal. Extracting one is deferred until a second backend needs it.
- The Sprint 05 deliverable list is interpreted as "renderer foundation", not
  as a mandate to create an empty module.

## Alternatives considered

- **Put `RenderNode` in core.** Rejected: it would import `DomNode` into core
  or require a parallel abstraction with no second implementation.
- **Create `dejaview-renderer` now.** Rejected by ADR-0003: one backend does
  not justify the module, and extraction later is mechanical.
- **Resolve components in core.** Rejected: resolution needs a document to
  create nodes, which is platform-specific. Core only exposes the lazy node.

## Revisit

When SSR, hydration or a second rendering backend is implemented. At that point
the platform-neutral parts of the render tree can move to a shared module.
