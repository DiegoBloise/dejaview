# Implementation Status

**Version:** 0.1.0-SNAPSHOT  
**Milestone:** Phase 2 — Reactivity (signals, effects, computed, scheduling, scopes)  
**Last updated:** 2026-11-11

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
| Element, text and fragment nodes | `dejaview-core` | JUnit tests |
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
| Error propagation without corrupting the scheduler | `dejaview-reactivity` | JUnit tests |
| Reactive counter update in the browser | example | Playwright E2E |
| Platform-neutral DOM contracts | `dejaview-dom` | JUnit tests via fake DOM |
| First-mount renderer | `dejaview-dom` | JUnit tests |
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
  → Effect rerun
  → DOM text update
```

The example is `examples/counter`. It renders a heading, a counter, a button and
a tagline. Clicking the button updates a `Signal<Integer>`; a tracked `Effect`
reruns and updates the DOM text.

---

## Deliberately not implemented yet

| Area | Status | Notes |
|---|---|---|
| Topological dependency ordering | Planned (Phase 2 refinement) | FIFO scheduling is implemented; glitch-free topological propagation is not |
| Equality short-circuit for computed values | Planned (Phase 2 refinement) | Invalidated computeds always notify dependents |
| Renderer-owned reactive bindings | Planned (Sprint 06) | Example still calls `Dom.byId` inside its effect |
| Automatic ownership tree | Planned (Sprint 07) | `ReactiveScope` is explicit; components do not own scopes yet |
| Component system / lifecycle | Planned (Sprint 07) | No `@Component` yet |
| Inputs, outputs, slots | Planned (Phase 3) | |
| Diffing / keyed updates | Planned | Current renderer is first-mount only |
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
5. **Direct DOM access in the example.** The effect calls
   `Dom.byId("count").setText(...)`. This is a bridge until the renderer owns
   reactive bindings, not the intended long-term API.
6. **Reentrant updates are rejected.** An effect cannot write to a signal it
   depends on, and an observer invalidated while running throws. Mutual update
   loops are detected by the scheduler's execution limit. Computed cycles fail
   with `IllegalStateException`.
7. **Reactive primitives are not thread-safe.** They target the single-threaded
   browser runtime and single-threaded JVM tests.
8. **First mount only.** `DomRenderer` appends a view tree to a container. It
   does not diff, patch, unmount or hydrate.
9. **No component abstraction.** `Views` factories are a convenience layer over
   immutable records, not a component system.
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

**Finish the Phase 2 refinements, then move to Sprint 05/06 — renderer and
reactive DOM bindings.**

Sprint 04 is implemented and tested: `Signal<T>`, `Effect`, lazy `Computed<T>`,
dependency tracking, re-collection, cycle detection, batching, synchronous FIFO
scheduling, effect cleanup, loop protection and `ReactiveScope` disposal. The
remaining reactivity work is:

1. Equality short-circuiting so dependents do not rerun when a computed value is
   unchanged.
2. Optional topological ordering for glitch-free computed propagation.
3. Renderer-owned reactive bindings, removing `Dom.byId` from application code.
4. A browser E2E test where a signal update reaches the DOM through a renderer
   binding rather than a manual lookup.

The reactivity module stays independent of the DOM. The renderer will subscribe
to effects and perform targeted text updates, keeping `dejaview-core` free of
browser APIs and preparing the ground for the component model in Sprint 07.
