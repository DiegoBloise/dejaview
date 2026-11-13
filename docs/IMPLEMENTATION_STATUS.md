# Implementation Status

**Version:** 0.1.0-SNAPSHOT  
**Milestone:** Phase 4 — Application Framework  
**Last updated:** 2026-11-13

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
| Stable attribute/property reconciliation (unchanged attributes are not re-applied, so unrelated re-renders never reload embedded frames or reset form state) | `dejaview-dom` | JUnit tests |
| Browser DOM adapter | `dejaview-dom` | Playwright E2E |
| SVG elements created in the SVG namespace (logos and icons paint instead of receiving only a layout box) | `dejaview-dom` | Playwright E2E (asserts `namespaceURI`) |
| Browser rendering | example | Playwright E2E |
| Click event handling | example | Playwright E2E |
| Generated browser bundle | example | `target/webapp/app.js` |
| Source maps | example | `app.js.map` |
| Browser E2E test | `examples/counter`, `examples/showcase`, `examples/website` | `-Pe2e` Failsafe test (Chromium via Playwright) |
| Official website (landing, user guide, playground) rendered by DejaView itself, in English and Portuguese | `examples/website` + `website/` | `WebsiteBrowserIT` (`-Pe2e`); static shells load the compiled bundle |
| Interactive playground with pre-compiled examples (counter, greeting, composition, conditional, usercard, list, router, http, forms) | `examples/website` + `website/playground/` | Rendered by DejaView, zero CDN dependencies; Java highlighting and overlay editor are site assets; examples execute real TeaVM bundles |
| Showcase module (website data) | `examples/showcase` | Pre-compiled TeaVM bundle used by the playground iframes |
| Showcase examples: `usercard` (inputs/outputs/slots/lifecycle) and `list` (keyed reconciliation) | `examples/showcase` | Playwright E2E in Chromium |
| `InjectionToken<T>` (identity-based; canonical `of(Class)` overload; diagnostic `name()`) | `dejaview-di` | JUnit tests |
| `InjectorBuilder` / live `injector.bind(...)` with fail-fast `duplicate-binding` | `dejaview-di` | JUnit tests |
| `InjectorBuilder.build()` copies registrations; builder reuse yields independent injectors | `dejaview-di` | JUnit tests |
| Three scopes: `SINGLETON` (one per registering injector), `SCOPED` (one per resolving injector), `TRANSIENT` (never cached) | `dejaview-di` | JUnit tests |
| Hierarchical injectors: `child(String)`/`child()`, nearest registration wins, live shadowing, no cross-sibling visibility | `dejaview-di` | JUnit tests |
| `inject()`/`has()` API (class-token and explicit-token resolution) | `dejaview-di` | JUnit tests |
| Circular dependency detection on a thread-local resolution path (`circular-dependency`, chain `A -> B -> A`, fails before cache mutation, works across injectors) | `dejaview-di` | JUnit tests |
| `missing-provider` errors naming token, injector and in-flight chain; `provider-error` wrapping with preserved cause | `dejaview-di` | JUnit tests |
| `AutoCloseable` disposal in reverse creation order through `ResourceRegistry`; children close before parent; idempotent `close()`; transients and `toInstance(...)` constants never injector-disposed | `dejaview-di` | JUnit tests |
| Explicit constructor-injection strategy via `Provider<T>` factories (`Injector -> T`); TeaVM-safe, no reflection | `dejaview-di` | JUnit tests |
| Keyed component replacement at a single position (a `ComponentView` whose `key` differs from the mounted node's key replaces the node; keys are component identity, like in keyed lists) | `dejaview-dom` | JUnit tests |
| `Route` definitions (literal, `:param`, `**`; nested and index routes; fail-fast `invalid-route` validation; JVM-unique stable ids `route:N`) | `dejaview-router` | JUnit tests |
| Route matching, parameters and child-wins parameter merge | `dejaview-router` | JUnit tests |
| Query parameters (first-wins repeated keys, `+` as space, percent-decoding; `%2F` never splits segments) | `dejaview-router` | JUnit tests |
| Nested routes and index (`""`) children | `dejaview-router` | JUnit tests |
| `Router` API (`navigate`/`replace`/`back`/`forward`) with resolve-before-push semantics; denied or unmatched navigations leave state and history unchanged | `dejaview-router` | JUnit tests |
| Route guards (`Allow`/`Deny`/`Redirect`), evaluated root-first with prefix-accumulated context | `dejaview-router` | JUnit tests |
| Route-declared and guard-returned redirects, re-resolved from scratch, ten-hop `redirect-loop` budget | `dejaview-router` | JUnit tests |
| `RouteMatch` (chain, params, query parameters, merged metadata) and route-level metadata | `dejaview-router` | JUnit tests |
| `notFound(...)` chain matched as a normal chain (guards included); no-match is a `false` result with a `Cancelled` event, not an exception | `dejaview-router` | JUnit tests |
| `NavigationEvent`s (`Started`/`Redirected`/`Ended`/`Cancelled`); `events()` subscription with reversible handle; `Ended` fires after commit | `dejaview-router` | JUnit tests |
| `RouterOutlet` at chain levels (0 default), keyed by the matched route's id; path parameters pushed as inputs; layouts embed the next level | `dejaview-router` | JUnit tests |
| `MemoryRouterHistory` (default history; seeded stack; back/forward notify listeners) | `dejaview-router` | JUnit tests |
| `BrowserHistory` (`pushState`/`replaceState` + `popstate` adoption) | `dejaview-router` | Playwright E2E |
| Router showcase example (`?example=router`): nested navigation, route params, browser back/forward restored by popstate adoption | `examples/showcase` | Playwright E2E |
| Router example published in the playground | `website/playground` | Static bundle served by the showcase |
| `HttpRequest` (`get`/`post`/`put`/`patch`/`delete` factories; fluent withers; percent-encoded query parameters, `+` for space) | `dejaview-http` | JUnit tests |
| `HttpHeaders` (immutable, case-insensitive names, multi-valued entries, insertion order) | `dejaview-http` | JUnit tests |
| `HttpClient` (async exactly-once delivery; registration-ordered request/response interceptors; per-request vs client timeouts and credentials; once-only guarantee) | `dejaview-http` | JUnit tests |
| `HttpResult` sealed (`Success`/`Failure`); HTTP error statuses are `Success`; `ok()` is 2xx | `dejaview-http` | JUnit tests |
| `HttpCall` cancellation (XHR `abort()` → `cancelled`); `isDone()` reflects delivery to the consumer | `dejaview-http` | JUnit tests |
| `JsonValue` tree + parser/writer (dependency-free; finite numbers; 512-level depth cap; offsets on errors) | `dejaview-http` | JUnit tests |
| `BrowserHttpTransport` (TeaVM XHR adapter; `timeout`/`withCredentials` via one-line JS interop; status 0 → `network-error`; Fetch deliberately not used — see ADR-0018) | `dejaview-http` | Chromium E2E |
| HTTP showcase example (`?example=http`), fetches `ping.json` and renders the body | `examples/showcase` | Playwright E2E |
| HTTP example published in the playground | `website/playground` | Static bundle served by the showcase |
| `FormControl`, `FormGroup`, `FormArray` with value, `touched`/`dirty`/`disabled`, aggregated `status()`, prefixed `errors()` | `dejaview-forms` | JUnit tests |
| Synchronous validators (`required`, `minLength`, `maxLength`, `email`, `pattern`, `min`, `max`, `custom`) | `dejaview-forms` | JUnit tests |
| Asynchronous validators with generation-counter staleness rejection; sync validators run first, async only on sync success | `dejaview-forms` | JUnit tests |
| Child error prefixing (`email.required`, `tags.0.required`); disabled children excluded from group/array values; `reset()`/`dispose()` semantics | `dejaview-forms` | JUnit tests |
| `FormBindings`: `input`/`textarea` (input), `checkbox`/`select` (change), blur → touched, `aria-invalid`/`disabled` reflect status | `dejaview-forms` | JUnit tests |
| Forms showcase example (`?example=forms`): sync + async validation, dynamic array, submission snapshot | `examples/showcase` | Playwright E2E |
| Forms example published in the playground | `website/playground` | Static bundle served by the showcase |
| Feature card, bilingual tutorial section (14–18), playground list update | `examples/website` | Playwright E2E |

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
| Routing | Implemented (Sprint 10) | See implemented table; bundle-level lazy route loading is deferred to Sprint 15 |
| HTTP client | Implemented (Sprint 11) | See implemented table; Fetch adapter deferred — XHR transport shipped instead (ADR-0018) |
| Reactive forms | Implemented (Sprint 12) | See implemented table; `dejaview-forms` with controls, groups, arrays, sync/async validators, stable error keys, DOM bindings and E2E coverage (ADR-0019) |
| State management | Planned | |
| SSR | Planned (Phase 6) | |
| SSG | Planned (Phase 6) | |
| Hydration | Planned (Phase 6) | |
| CLI | Planned (Phase 5) | |
| DevTools | Planned | |
| Spring Boot integration | Planned (Phase 8) | |
| Testing utilities | Planned | JUnit/Playwright are used directly |
| Field/annotation injection (`@Inject`, qualifiers) | Not started | The supported DI surface is the programmatic factory API (ADR-0016) |
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
   component keeps its instance while its position is reused. Distinct keys —
   including at a single child position — or class tokens
   (`componentFactory(Class, Supplier)`) swap the instance; unkeyed tokenless
   views reuse positionally, so dynamic swaps without keys or tokens keep
   the previous instance. There is no state migration.
10. **Nine examples across two modules.** `examples/counter` (one,
    class-based counter) and `examples/showcase` (counter, greeting,
    composition, conditional, usercard, list, router, http, forms), plus the conceptual previews in
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
15. **Dependency injection is not wired into the component system yet.**
    Components receive dependencies through plain constructor parameters
    resolved at the composition root. The injector is lazy, not
    thread-safe, and has no `validate()` startup check; components cannot
    create automatic child scopes per view.
16. **Routing is client-side only and not lazily split by bundle.** Route
    components instantiate at activation (factories), but the TeaVM output is
    a single bundle, so no route module loads on demand (deferred to Sprint
    15). A guard that denies a browser-initiated navigation cannot revert the
    URL — the browser keeps the restored address while the outlet keeps the
    previous match.
17. **The router has no data layer.** Route activation does not fetch data;
    there is no loader or suspense concept.
18. **HTTP is async-only and XHR-based.** Delivery is consumer-callback
    exactly once; HTTP error statuses are `Success` (branch on `ok()`); XHR
    cannot omit same-origin cookies (`OMIT` and `SAME_ORIGIN` both map to
    `withCredentials=false`); response bodies arrive as text without
    streaming; `cancel()` settles asynchronously as `cancelled`. The Fetch
    API is not bound by `teavm-jso-apis` 0.16.0 — XHR is the deliberate
    transport (ADR-0018).

---

## Documentation map

| Document | Purpose |
|---|---|
| `AGENTS.md` | Working rules, file map and the feature → website → docs Definition of Done |
| `README.md` | Project overview, quick start, module status |
| `docs/ARCHITECTURE.md` | Long-term architecture specification |
| `docs/COMPILER.md` | TeaVM feasibility and compiler constraints |
| `docs/IMPLEMENTATION_STATUS.md` | This document |
| `docs/ADR/` | Architectural Decision Records |

Any new feature, API or behavior must ship with its user-facing surface: the
bilingual website (landing feature card, guide section in English and
Portuguese, playground example when interactive) and the internal documents in
this table. See `AGENTS.md` for the checklist and `docs/ROADMAP.md` §2.3–2.4 for
the sprint rule.

---

## Next milestone recommendation

**Sprint 13 — State Management** (see `docs/ROADMAP.md`).

Sprint 12 is implemented and tested: `dejaview-forms` with `FormControl`,
`FormGroup` and `FormArray` (value, `touched`/`dirty`/`disabled`, aggregated
`status()`, prefixed `errors()`), synchronous validators (`required`,
`minLength`, `maxLength`, `email`, `pattern`, `min`, `max`, `custom`),
asynchronous validators with generation-counter staleness rejection (sync runs
first, async only on sync success), `reset()` restores initial values and
clears touched/dirty, `dispose()` invalidates pending async work, `FormSubmission`
snapshots validity and value. `FormBindings` connects DOM controls (`input`,
`textarea`, `checkbox`, `select`) to controls via value events; blur marks
touched; `aria-invalid` and `disabled` reflect control state. The showcase
`FormsDemo` (`?example=forms`) demonstrates sync + async validation, a dynamic
tag array and a submission snapshot; `ShowcaseBrowserIT#formsExampleValidatesAndSubmits`
validates it in Chromium. The website feature card, tutorial section (headings
14–18 in both languages) and playground example are wired; `WebsiteBrowserIT`
asserts 10 feature cards and 9 playground examples. See ADR-0019.

Sprint 11 is implemented and tested: `dejaview-http` with immutable
`HttpRequest` (`get`/`post`/`put`/`patch`/`delete`, fluent withers,
percent-encoded query parameters with `+` for space), immutable
case-insensitive multi-valued `HttpHeaders`, `HttpResponse` (`ok()` for 2xx)
with a dependency-free `JsonValue` tree/parser/writer (finite numbers,
512-level depth cap, offsets on errors), and `HttpClient` with async
exactly-once consumer delivery, registration-ordered request and response
interceptors (response interceptors observe completed round-trips only, HTTP
error statuses included), per-request vs client-level timeout (`-1` =
disabled) and credentials (`SAME_ORIGIN` default) resolution, and
cancellation via `HttpCall`. Transport-level failures carry stable codes
(`network-error`, `timeout`, `cancelled`, `interceptor-error`,
`missing-transport`, `json-error`); consumer exceptions are not caught. The
browser adapter uses XHR — `teavm-jso-apis` 0.16.0 ships no Fetch binding —
with the documented consequences (same-origin cookies cannot be omitted;
status 0 means `network-error`; no streaming). See ADR-0018.

Sprint 10 is implemented and tested: `dejaview-router` with immutable `Route`
definitions (literal, `:param`, `**`; nested and index routes; stable JVM
ids), resolve-before-push navigation (`navigate`/`replace`/`back`/`forward`)
that leaves state and history unchanged on denial or no-match, guards evaluated
root-first with prefix-accumulated contexts (`Allow`/`Deny`/`Redirect`; ten-hop
`redirect-loop` budget), route-declared and guard-returned redirects, an
optional `notFound(...)` chain, `NavigationEvent`s with a reversible listener
(`Ended` fires after commit), `RouterOutlet` rendering at a chain level keyed
by the matched route's id (path parameters pushed as inputs), and browser
history behind the `RouterHistory` seam (`BrowserHistory` for TeaVM,
`MemoryRouterHistory` for the JVM). Single-position keyed component views now
replace the mounted instance when the key changes, which is how route
swaps dispose the previous screen. Lazy route loading is limited to
activation-time factory deferral — bundle-level code splitting is deferred to
Sprint 15. The router is not wired to the injector; route components receive
dependencies through plain constructor parameters at the composition root. See
ADR-0017.

Open items carried forward: documented wrong-type-listener failure mode for
outputs, and the absence of an `onMount()` hook (`onInit()` covers
pre-render; anything DOM-effectful must wait for a re-render hook); DI is
still not wired into the component system or the router's outlets.
