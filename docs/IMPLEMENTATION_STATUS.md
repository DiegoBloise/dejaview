# Implementation Status

**Version:** 0.1.0-SNAPSHOT  
**Milestone:** Phase 3 — Component system  
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
| Component view inputs, output listeners, slots and raw values | `dejaview-core` | JUnit tests |
| Optional reconciliation key on all views (`TextView`, `FragmentView`, `ElementView`, `ComponentView`) | `dejaview-core` | JUnit tests |
| `ComponentBindings` (inputs/slots/outputs, immutable copies) | `dejaview-core` | JUnit tests |
| Component contract (`render`, `name`, `bind`, `onInit`, `onUpdate`, `onDestroy`, `close`) | `dejaview-core` | JUnit tests |
| Component inputs with fail-fast validation (`input(String, Class)`, `inputOr`; codes `missing-input`/`invalid-input`) | `dejaview-core` | JUnit tests |
| Component outputs (`emit` + `ComponentView#on`), registration order, no-op when no listeners | `dejaview-core` | JUnit tests |
| Content projection (default and named slots; direct splice, no `SlotView`) | `dejaview-core` | JUnit tests |
| `AbstractComponent` resource ownership (`own`, `onDispose`) | `dejaview-core` | JUnit tests |
| Functional and class-based component factories (incl. class-token form `componentFactory(Class, Supplier)`) | `dejaview-core` | JUnit tests |
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
| One component instance per mounted node | `dejaview-dom` | JUnit tests via fake DOM |
| Component rendering and disposal | `dejaview-dom` | JUnit tests via fake DOM |
| Nested components and independent instance state | `dejaview-dom` | JUnit tests via fake DOM |
| Component-scoped reactive rendering | `dejaview-dom` | JUnit tests via fake DOM |
| In-place text, attribute and property updates | `dejaview-dom` | JUnit tests via fake DOM |
| Keyed list reconciliation (opt-in when any child carries a key; duplicate keys fail fast before DOM mutation) | `dejaview-dom` | JUnit tests via fake DOM |
| Conditional DOM re-anchor only when sibling order actually changed | `dejaview-dom` | JUnit tests via fake DOM |
| Component type swaps detected via class tokens (`componentType`); tokenless views keep positional reuse | `dejaview-dom` | JUnit tests via fake DOM |
| Component lifecycle order (`bind → onInit → render … bind → render → onUpdate … onDestroy → close`) | `dejaview-dom` | JUnit tests via fake DOM |
| Class and style attribute updates | `dejaview-dom` | JUnit tests via fake DOM |
| Plain `class`/`style` attributes for external CSS libraries | `dejaview-core`, `dejaview-dom` | JUnit tests via fake DOM |
| Child removal and node replacement | `dejaview-dom` | JUnit tests via fake DOM |
| Batched reactive reconciliation | `dejaview-dom`, `dejaview-reactivity` | JUnit tests |
| Effect and listener cleanup on disposal | `dejaview-dom` | JUnit tests via fake DOM |
| Reactive renderer with in-place reconciliation | `dejaview-dom` | JUnit tests |
| Browser DOM adapter | `dejaview-dom` | Playwright E2E |
| Browser rendering | example | Playwright E2E |
| Click event handling | example | Playwright E2E |
| Generated browser bundle | example | `target/webapp/app.js` |
| Source maps | example | `app.js.map` |
| Browser E2E test | `examples/counter`, `examples/showcase` | `-Pe2e` Failsafe test (Chromium via Playwright) |
| Official website (landing, docs, status) | `website/` | Static site served over HTTP; sibling playground iframe |
| Interactive playground with pre-compiled examples (counter, greeting, composition, conditional, usercard, list) | `website/playground/` | Static HTML/CSS/JS, zero CDN dependencies; Java highlighting and overlay editor are inlined; examples execute real TeaVM bundles |
| Showcase module (website data) | `examples/showcase` | Pre-compiled TeaVM bundle used by the playground iframes |
| Showcase examples: `usercard` (inputs/outputs/slots/lifecycle) and `list` (keyed reconciliation) | `examples/showcase` | Playwright E2E in Chromium |

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
  → reconciliation (positional; keyed for children with keys)
  → DOM text and attribute update
```

The example is `examples/counter`. `CounterApp` mounts a class-based
`CounterComponent` through `Views.componentFactory(...)`. The component owns a
`Signal<Integer>` and renders a heading, a counter, a button and a tagline.
Clicking the button updates the signal; the mounted tree's effect reruns,
reconciles the component and updates the existing text node and the counter's
`data-count` attribute without recreating the component instance.

---

## Deliberately not implemented yet

| Area | Status | Notes |
|---|---|---|
| Topological dependency ordering | Planned (Phase 2 refinement) | FIFO scheduling is implemented; glitch-free topological propagation is not |
| Equality short-circuit for computed values | Planned (Phase 2 refinement) | Invalidated computeds always notify dependents |
| Keyed list reconciliation | Implemented | Children lists switch to keyed reconciliation as soon as one child carries a `key`; without keys, matching is positional |
| Styling library compatibility suite | Planned (Sprint 23) | Elements already emit plain `class`/`style` attributes; Bootstrap, Tailwind and daisyUI are not covered by automated tests yet |
| Fine-grained binding nodes | Not planned yet | Reactive updates are component-scoped: a signal read while rendering reruns the component and reconciles its subtree |
| Automatic reactive-scope ownership | Planned | Components own disposable resources via `AbstractComponent`, but do not create or own a `ReactiveScope` automatically |
| Component annotations (`@Component`, `@Input`, `@Output`) | Not started | The programmatic API is the supported surface; annotations remain a proposal layered on top of it |
| Annotations for lifecycle (`@OnInit` etc.) | Not started | `onInit`/`onUpdate`/`onDestroy` are the supported hooks |
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
   child scopes together. Components own disposable resources through
   `AbstractComponent`, but they do not create or own a `ReactiveScope`
   automatically.
5. **Reactive updates are component-scoped.** Signals read while rendering rerun
   the mounted effect and reconcile the component subtree. Fine-grained
   `BoundText`/`BoundAttribute` nodes are not part of the model yet.
6. **Reentrant updates are rejected.** An effect cannot write to a signal it
   depends on, and an observer invalidated while running throws. Mutual update
   loops are detected by the scheduler's execution limit. Computed cycles fail
   with `IllegalStateException`.
7. **Reactive primitives are not thread-safe.** They target the single-threaded
   browser runtime and single-threaded JVM tests.
8. **Reconciliation is positional unless a key is present.** Lists whose
   children carry no `key` reconcile by position; reordering such children can
   still recreate them. Lists with any keyed child reconcile by key. There is
   no public unmount API or hydration.
9. **Component identity is positional, or keyed, or typed.** A mounted
   component keeps its instance while its position is reused. Distinct keys or
   class tokens (`componentFactory(Class, Supplier)`) are the supported way to
   swap component classes at one position; tokenless views reuse positionally,
   so dynamic swaps without keys or tokens keep the previous instance. There
   is no state migration.
10. **Seven examples across two modules.** `examples/counter` (one,
    class-based counter) and `examples/showcase` (counter, greeting,
    composition, conditional, usercard, list), plus the conceptual previews in
    the playground that are not executable.
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

**Sprint 09 — Dependency Injection** (see `docs/ROADMAP.md`).

Sprint 08 is implemented and tested: typed inputs (`input(String, Class)` /
`inputOr`, fail-fast with `missing-input`/`invalid-input`), outputs (`emit` +
`ComponentView#on`, registration order, no-op when unlistened), default and
named slots (direct splice, no `SlotView`), explicit lifecycle
(`onInit`/`onUpdate`/`onDestroy` in mount/re-render/unmount order) and keyed
list reconciliation with token-based dynamic component replacement
(`componentFactory(Class, Supplier)`). The `usercard` and `list` showcase
examples exercise the new surface and are covered by Playwright E2E.

Fine-grained binding nodes and annotation-based `@Input`/`@Output` remain
future work; annotations should be layered on the programmatic API now in
place, not on a parallel surface.

Open items carried forward: documented wrong-type-listener failure mode for
outputs, and the absence of an `onMount()` hook (`onInit()` covers
pre-render; anything DOM-effectful must wait for a re-render hook).
