# Implementation Status

**Version:** 0.1.0-SNAPSHOT  
**Milestone:** Foundation (Phase 1, partial)  
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
  → DOM text update
```

The example is `examples/counter`. It renders a heading, a counter, a button and
a tagline. Clicking the button increments the counter and updates the DOM.

---

## Deliberately not implemented yet

| Area | Status | Notes |
|---|---|---|
| Signals / computed / effects | Planned (Phase 2) | The example updates the DOM manually |
| Reactive dependency tracking | Planned (Phase 2) | |
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

1. **No reactivity.** `CounterApp` calls `Dom.byId("count").setText(...)` after a
   click. This is an intentional first-milestone shortcut, not the intended
   long-term API.
2. **First mount only.** `DomRenderer` appends a view tree to a container. It
   does not diff, patch, unmount or hydrate.
3. **No component abstraction.** `Views` factories are a convenience layer over
   immutable records, not a component system.
4. **Single example.** Only the counter example exists.
5. **Browser coverage.** The E2E test covers Chromium. Firefox and WebKit are
   not part of the automated matrix yet.
6. **JVM/browser API parity.** Only APIs exercised by the current code are
   validated against TeaVM's class library.
7. **No production build profile.** The example uses readable, non-minified
   output for inspection. Minification and advanced optimization are not yet
   benchmarked.
8. **E2E requires network on first run.** Playwright downloads Chromium into
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

**Phase 2 — Reactivity Engine.**

The next vertical slice should replace the manual DOM update with a minimal,
tested reactivity core:

1. `Signal<T>` with `get`, `set`, `update`.
2. `Computed<T>` with lazy evaluation and caching.
3. `Effect` with automatic dependency tracking and disposal.
4. A scheduler/batching boundary that is platform-independent.
5. JVM unit tests for dependency graphs, cycles and cleanup.
6. A browser E2E test where a signal update changes the counter without manual
   DOM access.

This should remain independent of the DOM: the renderer subscribes to effects
and performs targeted text updates. That keeps `dejaview-core` free of browser
APIs and prepares the ground for the component model in Phase 3.
