# Implementation Status

**Version:** 0.1.0-SNAPSHOT  
**Milestone:** Phase 2 — Reactivity (first slice)  
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
| `Computed` values | Planned (Phase 2) | No lazy derived values yet |
| Batching / scheduler | Planned (Phase 2) | Effects run synchronously per change |
| Effect ownership / scopes | Planned (Phase 2) | Effects are disposed manually |
| Renderer-owned reactive bindings | Planned (Phase 2) | Example still calls `Dom.byId` inside its effect |
| Component system / lifecycle | Planned (Phase 3) | No `@Component` yet |
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

1. **Minimal reactivity only.** `Signal` and `Effect` exist, but there is no
   `Computed`, batching, scheduler or automatic effect ownership. Effects run
   synchronously on every changed value.
2. **Direct DOM access in the example.** The effect calls
   `Dom.byId("count").setText(...)`. This is a bridge until the renderer owns
   reactive bindings, not the intended long-term API.
3. **Reentrant updates are rejected.** An effect cannot write to a signal it
   depends on, and an effect invalidated while running throws. Batching and
   cycle handling are future work.
4. **Effects are not thread-safe.** They target the single-threaded browser
   runtime and single-threaded JVM tests.
5. **First mount only.** `DomRenderer` appends a view tree to a container. It
   does not diff, patch, unmount or hydrate.
6. **No component abstraction.** `Views` factories are a convenience layer over
   immutable records, not a component system.
7. **Single example.** Only the counter example exists.
8. **Browser coverage.** The E2E test covers Chromium. Firefox and WebKit are
   not part of the automated matrix yet.
9. **JVM/browser API parity.** Only APIs exercised by the current code are
   validated against TeaVM's class library.
10. **No production build profile.** The example uses readable, non-minified
    output for inspection. Minification and advanced optimization are not yet
    benchmarked.
11. **E2E requires network on first run.** Playwright downloads Chromium into
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

**Finish Phase 2 — Reactivity Engine.**

The first slice is implemented and tested: `Signal<T>`, synchronous `Effect`,
dependency tracking, dependency re-collection and disposal. The remaining work
is:

1. `Computed<T>` with lazy evaluation, caching and invalidation.
2. A platform-independent scheduler/batching boundary.
3. Ownership/scopes so effects are disposed with their component.
4. Renderer-owned reactive bindings, removing `Dom.byId` from application code.
5. JVM unit tests for dependency graphs, cycles, batching and cleanup.
6. A browser E2E test where a signal update reaches the DOM through a renderer
   binding rather than a manual lookup.

The reactivity module stays independent of the DOM. The renderer will subscribe
to effects and perform targeted text updates, keeping `dejaview-core` free of
browser APIs and preparing the ground for the component model in Phase 3.
