# Implementation Status

**Version:** 0.1.0-SNAPSHOT  
**Milestone:** Phase 2 — Reactivity and rendering foundations  
**Last updated:** 2026-11-12

This document separates what is implemented and tested from what is designed
or planned. If a capability is not listed as implemented here, it should not be
treated as available.

---

## Implemented and tested

| Capability | Module | Validation |
|---|---|---|
| Maven multi-module build | root | `./mvnw clean verify` |
| Maven Wrapper | root | `./mvnw` uses Maven 3.9.16 |
| Centralized dependency/plugin management | root | parent POM |
| Java 25 compilation | all | `maven.compiler.release=25` |
| Java → JavaScript compilation | example | TeaVM 0.16.0, `package` phase |
| Immutable view model | `dejaview-core` | JUnit tests |
| Framework configuration (`FrameworkConfig`) | `dejaview-core` | JUnit tests |
| Application context with owned resources | `dejaview-core` | JUnit tests |
| Shared `ResourceRegistry` with reverse-order disposal | `dejaview-core` | JUnit tests |
| Lifecycle contracts | `dejaview-core` | JUnit tests |
| `DejaViewException` with code and preserved cause | `dejaview-core` | JUnit tests |
| Logging contracts (`Logger`, `Loggers`) | `dejaview-core` | JUnit tests |
| Internal argument checks | `dejaview-core` | JUnit tests |
| Element, text and fragment nodes | `dejaview-core` | JUnit tests |
| Component view node (`ComponentView`) | `dejaview-core` | JUnit tests |
| Attribute and event bindings | `dejaview-core` | JUnit tests |
| `Signal<T>` (`get`, `set`, `update`, equality no-op) | `dejaview-reactivity` | JUnit tests |
| Synchronous `Effect` with dependency tracking and disposal | `dejaview-reactivity` | JUnit tests |
| Dependency re-collection across effect runs | `dejaview-reactivity` | JUnit tests |
| Lazy `Computed<T>` with cache and push-based invalidation | `dejaview-reactivity` | JUnit tests |
| Computed dependency re-collection and computed-on-computed graphs | `dejaview-reactivity` | JUnit tests |
| Circular computed dependency detection | `dejaview-reactivity` | JUnit tests |
| `Computed` disposal and post-disposal failure | `dejaview-reactivity` | JUnit tests |
| Explicit batching with deduplicated effect runs | `dejaview-reactivity` | JUnit tests |
| Synchronous FIFO scheduler and execution ordering | `dejaview-reactivity` | JUnit tests |
| Infinite reactive loop protection | `dejaview-reactivity` | JUnit tests |
| Effect cleanup callbacks before re-execution and disposal | `dejaview-reactivity` | JUnit tests |
| `ReactiveScope` ownership and grouped disposal | `dejaview-reactivity` | JUnit tests |
| Shared disposal behavior between contexts and scopes | `dejaview-core`, `dejaview-reactivity` | JUnit tests |
| Error propagation without corrupting the scheduler | `dejaview-reactivity` | JUnit tests |
| Reactive counter update in the browser | example | Playwright E2E |
| Reactive attribute update in the browser | example | Playwright E2E |
| Platform-neutral DOM contracts | `dejaview-dom` | JUnit tests via fake DOM |
| Runtime configuration (`RuntimeConfig`) | `dejaview-dom` | JUnit tests |
| Browser environment detection | `dejaview-dom` | JUnit test on JVM; E2E in Chromium |
| Reversible DOM event registration | `dejaview-dom` | JUnit tests via fake DOM |
| Runtime start/shutdown with mounted-tree disposal | `dejaview-dom` | JUnit tests |
| Explicit render tree (`RenderNode` variants) | `dejaview-dom` | JUnit tests via fake DOM |
| Renderer and reconciliation contracts | `dejaview-dom` | JUnit tests |
| Component rendering and disposal | `dejaview-dom` | JUnit tests via fake DOM |
| Component-scoped reactive rendering | `dejaview-dom` | JUnit tests via fake DOM |
| In-place text, attribute and property updates | `dejaview-dom` | JUnit tests via fake DOM |
| Class and style attribute updates | `dejaview-dom` | JUnit tests via fake DOM |
| Child removal and node replacement | `dejaview-dom` | JUnit tests via fake DOM |
| Batched reactive reconciliation | `dejaview-dom`, `dejaview-reactivity` | JUnit tests |
| Effect and listener cleanup on disposal | `dejaview-dom` | JUnit tests via fake DOM |
| Reactive renderer with in-place reconciliation | `dejaview-dom` | JUnit tests |
| Browser DOM adapter | `dejaview-dom` | Playwright E2E |
| Browser rendering | example | Playwright E2E |
| Click event handling | example | Playwright E2E |
| Generated browser bundle | example | `target/webapp/app.js` |
| Source maps | example | `app.js.map` |
| Browser E2E test | example | `-Pe2e` Failsafe test |

### End-to-end proof

The following chain is validated by automated tests:

```
Java source
  → Maven build
  → TeaVM 0.16.0
  → app.js
  → Chromium
  → DOM rendering
  → click event
  → Signal update
  → render effect
  → positional reconciliation
  → DOM text and attribute update
```

The example is `examples/counter`. It renders a heading, a counter, a button and
a tagline. Clicking the button updates a `Signal<Integer>`; the mounted tree's
effect reruns, reconciles the component view and updates the existing text node
and the counter's `data-count` attribute.

---

## Deliberately not implemented yet

| Area | Status | Notes |
|---|---|---|
| Topological dependency ordering | Planned (Phase 2 refinement) | FIFO scheduling is implemented; glitch-free topological propagation is not |
| Equality short-circuit for computed values | Planned (Phase 2 refinement) | Invalidated computeds always notify dependents |
| Keyed list reconciliation | Planned | Positional matching preserves nodes for unchanged positions, but moving or reordering children can recreate them |
| Fine-grained binding nodes | Not planned yet | Reactive updates are component-scoped: a signal read while rendering reruns the component and reconciles its subtree |
| Automatic ownership tree | Planned (Sprint 07) | `ReactiveScope` is explicit; components do not own scopes yet |
| Component system / lifecycle | Planned (Sprint 07) | No `@Component` yet |
| Inputs, outputs, slots | Planned (Phase 3) | |
| Hydration | Planned (Phase 6) | |
| Routing | Planned (Phase 4) | |
| Dependency injection | Planned (Phase 4) | |
| HTTP client | Planned (Phase 4) | |
| Reactive forms | Planned (Phase 4) | |
| State management | Planned | |
| SSR | Planned (Phase 6) | |
| SSG | Planned (Phase 6) | |
| Hydration | Planned (Phase 6) | |
| CLI | Planned (Phase 5) | |
| DevTools | Planned | |
| Spring Boot integration | Planned (Phase 8) | |
| Testing utilities | Planned | JUnit/Playwright are used directly |
| Annotation processing | Not started | |
| Compiler abstraction layer | Not started | TeaVM is configured directly in Maven |

---

## Known limitations

1. **Reactivity is synchronous and single-threaded.** Batching is explicit via
   `Reactive.batch(...)`; there is no microtask, time-based or cross-thread
   scheduler.
2. **Computed over-execution.** Invalidation is push-based and evaluation is
   pull-based. A computed marks itself dirty and notifies its observers as soon
   as a dependency changes; if an effect reads the computed and the recomputed
   value turns out to be equal, the effect still reruns. Equality
   short-circuiting is future work (ADR-0009).
3. **No topological dependency ordering.** Effects run in invalidation order
   (FIFO). Glitch-free topological propagation of computed values is future
   work.
4. **Ownership is explicit.** `ReactiveScope` disposes effects, computeds and
   child scopes together, but components do not own scopes automatically yet.
5. **Reactive updates are component-scoped.** Signals read while rendering rerun
   the mounted effect and reconcile the component subtree. Fine-grained
   `BoundText`/`BoundAttribute` nodes are not part of the model yet.
6. **Reentrant updates are rejected.** An effect cannot write to a signal it
   depends on, and an observer invalidated while running throws. Mutual update
   loops are detected by the scheduler's execution limit. Computed cycles fail
   with `IllegalStateException`.
7. **Reactive primitives are not thread-safe.** They target the single-threaded
   browser runtime and single-threaded JVM tests.
8. **Positional reconciliation only.** `DomTreeReconciler` preserves nodes at
   matching positions, but moving or reordering children can recreate them.
   There is no keyed diff, public unmount API or hydration.
9. **No component abstraction.** `ComponentView` is a reactive rendering
   boundary; `Views` factories are a convenience layer over immutable records.
   There is no component system yet.
10. **Single example.** Only the counter example exists.
11. **Browser coverage.** The E2E test covers Chromium. Firefox and WebKit are
    not part of the automated matrix yet.
12. **JVM/browser API parity.** Only APIs exercised by the current code are
    validated against TeaVM's class library.
13. **No production build profile.** The example uses readable, non-minified
    output for inspection. Minification and advanced optimization are not yet
    benchmarked.
14. **E2E requires network on first run.** Playwright downloads Chromium into
    the user's cache. The profile is opt-in for that reason.

---

## Documentation map

| Document | Purpose |
|---|---|
| `README.md` | Project overview, quick start, module status |
| `docs/ARCHITECTURE.md` | Long-term architecture specification |
| `docs/COMPILER.md` | TeaVM feasibility and compiler constraints |
| `docs/IMPLEMENTATION_STATUS.md` | This document |
| `docs/ADR/` | Architectural Decision Records |

---

## Next milestone recommendation

**Sprint 07 — Component Model and Composition.**

Sprint 06 is implemented and tested: the renderer owns a reactive effect,
reconciles text, attributes, properties, class/style attributes, events and
children in place, and disposes effects and listeners with the mounted tree. The
counter example no longer touches the DOM directly. The remaining reactivity
refinements are equality short-circuiting and optional topological ordering;
they are not blockers for the component model.

Sprint 07 should build component lifecycle, ownership and composition on top of
`ComponentView`. Fine-grained binding nodes and keyed list reconciliation remain
future work and should not be added before a concrete need appears.
