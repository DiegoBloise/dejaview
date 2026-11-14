# Implementation Status

**Version:** 0.1.0-SNAPSHOT  
**Milestone:** Phase 6 — Server Rendering and Static Generation (in progress)  
**Last updated:** 2026-11-14

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
| Browser E2E test | `dejaview-cli`, `examples/counter`, `examples/showcase`, `examples/website`, `dejaview-compiler` | `-Pe2e` Failsafe test (Chromium via Playwright) |
| Official website (landing, user guide, playground) rendered by DejaView itself, in English and Portuguese | `examples/website` + `website/` | `WebsiteBrowserIT` (`-Pe2e`); static shells load the compiled bundle |
| Interactive playground with pre-compiled examples (counter, greeting, composition, conditional, usercard, list, router, http, forms, state) | `examples/website` + `website/playground/` | Rendered by DejaView, zero CDN dependencies; Java highlighting and overlay editor are site assets; examples execute real TeaVM bundles |
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
| `StateStore`, named `Selector`, lazy store-owned `select()` projections, `Stores.combine` cross-store composition | `dejaview-state` | JUnit tests |
| `AsyncStore` with generation-counter staleness rejection and exactly-once completions; `AsyncState` (IDLE/LOADING/SUCCESS/FAILURE) keeping the last good value (stale-while-revalidate) | `dejaview-state` | JUnit tests |
| Structured `AsyncError` with stable machine-readable codes; `AsyncLoader` contract | `dejaview-state` | JUnit tests |
| `StateStorage` contract, `StateCodec` (`string`/`integer`, wrapped failures), `PersistedStore` restore-before-observers with write-through effect | `dejaview-state` | JUnit tests |
| `StateRegistry` composition-root registry with reverse-order `disposeAll()` | `dejaview-state` | JUnit tests |
| `internal/BrowserStorage` over `localStorage`/`sessionStorage`; `MemoryStateStorage` for tests and SSR | `dejaview-state` | JUnit tests (memory); browser adapter covered by E2E |
| State showcase example (`?example=state`): shared cart store, selectors, cross-store discount, async checkout, persisted theme | `examples/showcase` | Playwright E2E |
| State example published in the playground | `website/playground` | Static bundle served by the showcase |
| Feature card, bilingual tutorial sections, playground list updates | `examples/website` | Playwright E2E |
| `dejaview-cli` — `dejaview new` project generator (embedded `basic-app` template, Maven wrapper, standalone pom, placeholder substitution) | `dejaview-cli` | JUnit tests (`ProjectGeneratorTest`, `MainTest`) |
| `dejaview build` — wrapper orchestration with `[ERROR]` diagnostics and TeaVM failure hints (class file version, missing `teavm-classlib`, unresolved classes, strict mode) | `dejaview-cli` | JUnit tests (`BuildDiagnosticsTest`) |
| `dejaview run` — development server on `com.sun.net.httpserver`: static serving with containment check, content types, injected poll-based live reload, `/__dejaview__/version` | `dejaview-cli` | JUnit tests (`DevServerTest`); Chromium E2E |
| File watching (`WatchService`, recursive, 300 ms debounce) with automatic rebuilds; failed rebuilds serve the last good build | `dejaview-cli` | JUnit tests; Chromium E2E (`CliWorkflowIT`) |
| `dejaview doctor` — environment and project validation (JDK ≥ 25, compiler, wrapper, layout, `dejaview.properties`, local repo, port) | `dejaview-cli` | JUnit tests (`MainTest`) |
| `dejaview.properties` — per-project port/host/webappDir configuration; flags take precedence; line-numbered errors | `dejaview-cli` | JUnit tests (`ProjectConfigTest`) |
| Centralized TeaVM configuration (parent POM `pluginManagement`; browser modules declare only `mainClass`) | root + browser modules | `./mvnw clean verify` |
| `development`/`production` build profiles (`SIMPLE` + readable JS by default; `FULL` DCE + minified `-Pproduction`) | root | `-Pproduction` build; browser E2E suite green on optimized bundles |
| Webapp validation on every build (`WebAppValidator`/`VerifyWebApp`: bundle presence, non-empty, escapes-root, local reference resolution, size budget; failures report file + reference) | `dejaview-compiler` | JUnit tests (31); `./mvnw clean verify` reports 0 errors |
| Deterministic `dejaview-assets.json` fingerprint (paths, byte sizes, SHA-256; excludes itself; byte-identical across equivalent builds) | `dejaview-compiler` | JUnit tests; written by every build; `CliWorkflowIT` asserts it for generated apps |
| Reproducible output under equivalent build conditions (`project.build.outputTimestamp`; TeaVM determinism verified) | `dejaview-compiler` | `-Pe2e` `ReproducibleBuildIT` (two nested builds, byte comparison) |
| Build diagnostics with likely-cause hints (class file version, missing `teavm-classlib`, unresolved classes, strict mode) | `dejaview-cli` | JUnit tests (`BuildDiagnosticsTest`) |
| `dejaview-server` — platform contracts (`PlatformKind`, `Platform`, `ServerPlatform` singleton, `ServerTokens`), transport-neutral `ServerRequest`/`ServerResponse` values (no servlet/HTTP-framework types; not wired to a server) | `dejaview-server` | JUnit tests |
| `ServerContext` — request-scoped child injector (`server.request`), `REQUEST`/`CONTEXT` bound inside the child only, owned `ResourceRegistry` (reverse-order disposal, idempotent close), per-context `PlatformCompatibilityRegistry`; fresh `MemoryStateStorage` bound when the parent cannot resolve `StateStorage` | `dejaview-server` | JUnit tests (`ServerContextTest`, `ServerBindingsTest`) |
| `ServerBindings.apply(InjectorBuilder)` — `PLATFORM` to the server singleton, `StateStorage` with `Scope.SCOPED` (one `MemoryStateStorage` per resolving injector, i.e. per request) | `dejaview-server` | JUnit tests (`ServerBindingsTest`) |
| `PlatformCompatibility` + `BrowserOnlyComponent` marker + registry (exact-class override > marker by assignability > UNIVERSAL); SERVER_ONLY allowed on the server | `dejaview-server` | JUnit tests (`PlatformCompatibilityRegistryTest`) |
| `ServerRenderer` — renders the platform-neutral `View` tree into a sealed `ServerNode` tree (`ServerElement`/`ServerText`/`ServerFragment`); BROWSER_ONLY components rejected with `browser-only-component` before `bind`/`onInit`/`render` (pre-construction via class token); fragments splice; components get `onDestroy()` then `close()` in reverse creation order on success and failure alike; event handlers dropped and reported de-duplicated in `ServerTree.droppedEvents()` | `dejaview-server` | JUnit tests (`ServerRendererTest`, `ServerNodeTest`) |
| `SerializationBoundary` — strict `toJson`/`write` (null, JsonValue trees, String, Boolean, finite Numbers, String-keyed Maps, Lists; depth ≤ 512; rest rejected with `not-serializable`) | `dejaview-server` | JUnit tests (`SerializationBoundaryTest`) |
| `ServerErrorBoundary` — server-side containment: the failed subtree's components are destroyed and closed LIFO, one `BoundaryError` (original failure class) is recorded in `ServerTree.boundaryErrors()`, and `fallback(error)` renders in place; fallback failures propagate to the enclosing boundary; `isComplete()` is false when anything was contained | `dejaview-server` | JUnit tests (`ServerRendererTest`, 7 boundary cases) |
| Module purity — no DOM adapter and no TeaVM on the `dejaview-server` classpath | `dejaview-server` | JUnit test (`ModulePurityTest`) |
| `dejaview-server` module is built and tested with the rest of the reactor (`./mvnw clean verify`: 80 tests) | `dejaview-server` | `./mvnw clean verify` |
| `SsrRenderer` — page (`renderPage`/`writePage`) and fragment (`renderFragment`/`writeFragment`) rendering: `ServerRenderer` → `SsrValidator` → serializer; returns an `SsrResult` (status, HTML, dropped events, boundary reports, warnings, attached error) | `dejaview-ssr` | JUnit tests (`SsrRendererTest`) |
| `HtmlSerializer` / `HtmlPage` — escaped text (`&`, `<`, `>`) and attribute values (`"`, `'` added), void elements without closing tags, fragments spliced without wrappers, complete document with `lang`, charset, viewport, title, description, canonical and meta/Open Graph tags | `dejaview-ssr` | JUnit tests (`HtmlSerializerTest`, `HtmlPageTest`) |
| `SsrValidator` — rejects illegal tag/attribute names, void elements with children, duplicate attributes and depth > 512 (`ssr-validation-failed`); warns on `raw-text-escaped`, `skipped-property` and `empty-root` | `dejaview-ssr` | JUnit tests (`SsrValidatorTest`) |
| DOM property subset — `value` → `value` attribute; `checked`/`selected`/`disabled`/`readonly`/`multiple` → boolean attributes when `"true"`; explicit attribute wins; other properties skipped and reported | `dejaview-ssr` | JUnit tests (`HtmlSerializerTest`, `SsrValidatorTest`) |
| `PageMeta` — mutable, fluent, request-scoped metadata (`lang`, `title`, `description`, `canonical`, `meta`, `metaProperty`); `SsrResult.toResponse()` → `ServerResponse.html(status, html)` | `dejaview-ssr` | JUnit tests (`PageMetaTest`, `SsrRendererTest`) |
| Failure model — unhandled render/validation failure → `500` with a fixed generic document, exception attached to the result and never leaked; `isSuccess()` means `error == null` (host-chosen 404 pages are successful renders) | `dejaview-ssr` | JUnit tests (`SsrRendererTest`) |
| Module purity — no DOM adapter and no TeaVM on the `dejaview-ssr` classpath; `dejaview-ssr` module built and tested with the reactor (43 tests) | `dejaview-ssr` | JUnit test (`ModulePurityTest`) |
| Server-rendered example application — plain components, request-scoped `PageMeta`/`ServerContext`, GET search form, contained failing widget, 404 page, served by the JDK `HttpServer` on a loopback port | `examples/ssr` | JUnit tests (`SsrApplicationTest`, 10); Playwright E2E with JavaScript disabled (`SsrBrowserIT`, 4) |
| `dejaview-ssg` — static site generation over the SSR pipeline: `SsgSite`/`SsgRoute` (explicit `page`/`parametrized` declarations, eager path and parameter validation, duplicate-path and output-file collision detection), `SsgGenerator.generate` (plan → render → plan-assets → prepare-output → copy-assets → write-pages → sitemap → link-check), `SsgAsset` copy, `SsgPageContext`, `SsgResult`/`SsgPage`/`SsgWarning`, `sitemap.xml` excluding the 404 document | `dejaview-ssg` | JUnit tests (`SsgGeneratorTest`, `SsgRouteTest`, `SsgSiteTest`, `SsgPathsTest`, 45 total) |
| Output safety and deployability — pages rendered before anything is written (failures name the route); local `href`/`src` references validated against the written tree (percent-decoded; external URLs and fragments ignored); output cleaned by default, refusing filesystem roots and directories containing `pom.xml` | `dejaview-ssg` | JUnit tests (`SsgGeneratorTest`, 21; `ModulePurityTest` asserts purity) |
| Statically generated example website — component-based site (home, about, catalogue, one route per catalogue entry, stylesheet, 404 document) generated at `package` via `exec-maven-plugin` into `examples/ssg/target/site` | `examples/ssg` | JUnit-free JVM example; Playwright E2E with JavaScript disabled against a JDK static `HttpServer` on an ephemeral loopback port (`SsgBrowserIT`, 3) |
| CLI end-to-end workflow: generate → wrapper build → serve → Chromium render → source edit → rebuild → reload | `dejaview-cli` | Playwright E2E (`CliWorkflowIT`) |

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
| Routing | Implemented (Sprint 10) | See implemented table; bundle-level lazy code splitting is not supported by TeaVM (single bundle) and remains open — see ADR-0017 |
| HTTP client | Implemented (Sprint 11) | See implemented table; Fetch adapter deferred — XHR transport shipped instead (ADR-0018) |
| Reactive forms | Implemented (Sprint 12) | See implemented table; `dejaview-forms` with controls, groups, arrays, sync/async validators, stable error keys, DOM bindings and E2E coverage (ADR-0019) |
| State management | Implemented (Sprint 13) | See implemented table; `dejaview-state` with `StateStore`, named selectors, cross-store `Stores.combine`, `AsyncStore` with exactly-once completions, `PersistedStore`/`StateCodec` persistence and E2E coverage (ADR-0020) |
| SSR | Implemented (Sprint 17) | See implemented table; `dejaview-ssr` renders validated, escaped HTML documents and fragments with `ServerErrorBoundary` containment and per-request `PageMeta` (ADR-0024). Alpha scope: no state serialization, no hydration, no progressive streaming |
| SSG | Implemented (Sprint 18) | See implemented table; `dejaview-ssg` expands explicitly declared routes at build time through the SSR pipeline and writes a deployable HTML tree with copied assets, validated local references, `sitemap.xml` and clean-by-default output (ADR-0025). Alpha scope: no hydration, no state serialization, no incremental output; CLI integration deferred |
| Hydration | Planned (Sprint 19) | |
| CLI | Implemented (Sprint 14) | See implemented table; `dejaview-cli` with `new`/`build`/`run`/`doctor`, the development server with live reload and Chromium E2E coverage (`CliWorkflowIT`) (ADR-0021) | |
| DevTools | Planned | |
| Spring Boot integration | Planned (Phase 8) | |
| Testing utilities | Planned | JUnit/Playwright are used directly |
| Field/annotation injection (`@Inject`, qualifiers) | Not started | The supported DI surface is the programmatic factory API (ADR-0016) |
| Annotation processing | Not started | |
| Compiler abstraction and production build pipeline | Implemented (Sprint 15) | See implemented table; `dejaview-compiler` with centralized TeaVM configuration, development/production profiles, webapp validation, SHA-256 asset manifest and reproducible builds (ADR-0022) |

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
10. **Ten examples across two modules.** `examples/counter` (one,
    class-based counter) and `examples/showcase` (counter, greeting,
    composition, conditional, usercard, list, router, http, forms, state), plus the conceptual previews in
    the playground that are not executable.
11. **Browser coverage.** The E2E test covers Chromium. Firefox and WebKit are
    not part of the automated matrix yet.
12. **JVM/browser API parity.** Only APIs exercised by the current code are
    validated against TeaVM's class library.
13. **SSR is alpha and produces pages, not hydrated applications.**
    `dejaview-ssr` (ADR-0024) renders a complete, escaped HTML document from the
    same component model. It does not serialize state and emits no hydration
    markers (Sprint 19); the `Writer` variants render and validate the whole
    document before writing, so there is no progressive streaming (Phase 7).
    Only the `value` and boolean DOM properties are serialized; other properties
    and text inside `script`/`style` are skipped or escaped and reported as
    warnings. There is no deployable server adapter yet (Sprint 21), so the host
    owns its HTTP server — `examples/ssr` uses the JDK `HttpServer`. Events are
    still dropped during server rendering by design and reported de-duplicated
    on `ServerTree.droppedEvents()`; browser-only components are still rejected
    with `browser-only-component`; SERVER_ONLY classes are allowed, and
    enforcement on the browser side does not exist yet.
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
19. **Server-side DI is not wired into components.** The request injector is
    available to host code and to the renderer (for platform-compatibility
    classification), but components mounted on the server resolve dependencies
    through plain constructor parameters, like their browser counterparts.
    Numbers crossing `SerializationBoundary` are JSON doubles; integers above
    2^53 lose precision.

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

**Sprint 19 — Hydration** (see `docs/ROADMAP.md`).

Sprint 18 is implemented and tested: `dejaview-ssg`, a pure Java 25 module
(no browser APIs, no TeaVM; asserted by `ModulePurityTest`) that turns the
SSR pipeline into a build step (ADR-0025). Routes are declared explicitly in
Java — `SsgRoute.page` and `SsgRoute.parametrized` with whole-segment
`{name}` placeholders — because the framework has no reflection and no
annotation scanning. `SsgGenerator.generate` runs plan → render → plan-assets
→ prepare-output → copy-assets → write-pages → sitemap → link-check: pages
are rendered before anything is written (a failure names the route),
everything path- or asset-like is validated eagerly, duplicate paths and
page/asset collisions fail the build, the output directory is cleaned by
default (refusing filesystem roots and directories containing `pom.xml`), and
every local `href`/`src` in the written HTML is validated against the written
tree. `sitemap.xml` is emitted when `baseUrl` is set and excludes the 404
document. `examples/ssg` is a working static site generated at `package` via
`exec-maven-plugin` (7 pages + 1 asset) and validated in Chromium with
JavaScript disabled by `SsgBrowserIT`. 45 JVM tests cover `dejaview-ssg` and
3 browser tests cover the example. The bilingual website ships a sixteenth
feature card and a new guide section 21 (both languages). Not delivered in
this sprint: hydration and state serialization (Sprint 19), incremental or
parallel output, a `dejaview` CLI static command, and the full DOM property
set — the alpha limits are recorded in ADR-0025.

Sprint 17 is implemented and tested: `dejaview-ssr`, a pure Java 25 module
(no browser APIs, no TeaVM; asserted by `ModulePurityTest`) that renders the
`ServerNode` tree into validated, escaped HTML (ADR-0024). `SsrRenderer`
composes `ServerRenderer` → `SsrValidator` → serializer and returns an
`SsrResult` with the HTTP status, the HTML, the dropped events, boundary
reports, warnings and the attached failure; `renderPage` emits a complete
document and `renderFragment` a bare fragment, and the `Writer` variants write
the same output. `SsrValidator` rejects illegal tag or attribute names, void
elements with children, duplicate attributes and depth beyond 512 with
`ssr-validation-failed`, and reports `raw-text-escaped`, `skipped-property` and
`empty-root` as warnings; text and attribute values are always escaped, so
application data never becomes markup. `dejaview-server` gained
`ServerErrorBoundary`: a failure in the protected subtree destroys and closes
only that subtree, records a `BoundaryError` (original failure class) and
renders `fallback(error)` in place; fallback failures propagate outward.
`PageMeta` carries request-scoped lang, title, description, canonical and
meta/Open Graph tags into `HtmlPage`'s document skeleton. Unhandled failures
produce a generic `500` page and the exception is never leaked to the browser.
`examples/ssr` is a working server-rendered application (GET search form,
contained widget failure, 404 page) served by the JDK `HttpServer`.
43 JVM tests cover `dejaview-ssr`, 10 more cover the example, and
`SsrBrowserIT` renders it in Chromium with JavaScript disabled. The bilingual
website ships a fifteenth feature card and a new guide section 20 (both
languages). Not delivered in this sprint: state serialization and hydration
(Sprint 19), progressive streaming (Phase 7), a deployable server adapter
(Sprint 21) and the full DOM property set — the alpha limits are recorded in
ADR-0024.

Sprint 16 is implemented and tested: `dejaview-server`, a pure Java 25 module
with no browser APIs and no TeaVM (asserted by `ModulePurityTest`) that gives
the framework a server platform foundation capable of supporting SSR
(ADR-0023). The platform contracts are `PlatformKind`/`Platform`/
`ServerPlatform`, bound by `ServerBindings.apply` to `ServerTokens.PLATFORM`;
`ServerBindings` also binds `StateStorage` with `Scope.SCOPED`, giving one
`MemoryStateStorage` per request scope by default. Each request handled by a
host gets a `ServerContext`: a child injector (`server.request`) that binds
`REQUEST`/`CONTEXT` inside the child only, owns a `ResourceRegistry` disposed
in reverse registration order, and isolates state and component class
classifications per request; when the parent injector cannot resolve
`StateStorage`, the context binds a fresh `MemoryStateStorage` itself.
`ServerRenderer` renders the platform-neutral `View` tree into a sealed
`ServerNode` tree (`ServerElement`/`ServerText`/`ServerFragment`): browser-only
components — via the `BrowserOnlyComponent` marker, an exact-class registry
override, or an explicit `componentType` token (rejected before construction)
— fail with `browser-only-component` before `bind`/`onInit`/`render`; event
handlers are dropped and reported de-duplicated in
`ServerTree.droppedEvents()`; every mounted component receives `onDestroy()`
(when initialized) and `close()` in reverse creation order, on the success and
failure paths alike. Strict JSON leaves only via
`SerializationBoundary.toJson`/`write` (`not-serializable` otherwise, depth
cap 512). `ServerRequest`/`ServerResponse` are transport-neutral value types,
not wired to any HTTP server. The bilingual website shipped a fourteenth
feature card and guide section 19 (both languages) when the sprint was
delivered. 80 JVM tests cover the module today, including the Sprint 17
boundary support; HTML serialization arrived in Sprint 17 (ADR-0024). SSG
arrived in Sprint 18 (ADR-0025). Hydration (Sprint 19) and a deployable
server adapter (Sprint 21) remain open.

Sprint 14 is implemented and tested: `dejaview-cli` with
`dejaview new` (embedded `basic-app` template: generated project with the
Maven wrapper, a standalone pom, a counter component and the host page —
refuses non-empty directories, restores `.gitignore`, marks `mvnw` executable),
`dejaview build` (wrapper orchestration with `[ERROR]` diagnostics and
filtered noise), `dejaview run` (development server on
`com.sun.net.httpserver`: static serving with a containment check, correct
content types, `no-store` responses, HTML responses with the live-reload
client injected, `/__dejaview__/version` and `/__dejaview__/live.js`
endpoints; `WatchService`-based recursive watching with a 300 ms debounce;
rebuilds run inline so they never overlap; a failed rebuild keeps the last
good build serving), `dejaview doctor` (JDK ≥ 25, compiler, wrapper, layout,
`dejaview.properties`, local repo, port checks) and
`dejaview.properties` (`port`/`host`/`webappDir`; flags take precedence).
The whole loop is validated in Chromium by `CliWorkflowIT` (generate →
wrapper build → serve → render → source edit → rebuild → live reload). The
bilingual website documents the CLI in the guide (section 16, both languages)
and ships a twelfth feature card; the CLI has no playground example by
design (it is a terminal workflow, not a browser one). See ADR-0021.

Sprint 13 is implemented and tested: `dejaview-state` with `StateStore`
(shared, observable holder with tracked reads and synchronous writes), named
`Selector` projectors, lazy store-owned `select()` computeds, `Stores.combine`
for cross-store derived values, `StateRegistry` (anti-global composition-root
registry with reverse-order `disposeAll()`), `AsyncStore` (generation-counter
staleness rejection, exactly-once completions, stale-while-revalidate data),
`AsyncState`/`AsyncStatus`/`AsyncError` (stable machine-readable codes;
applications map codes to localized copy, like form error keys), and
persistence through `StateStorage` + `StateCodec` + `PersistedStore`
(restore-before-observers; write-through effect; decode/storage failures
surface as codes, never thrown). Browser storage ships as
`internal/BrowserStorage` over `localStorage`/`sessionStorage`; JVM tests use
`MemoryStateStorage`. The showcase `StateDemo` (`?example=state`) demonstrates
the shared cart store, selectors, a cross-store discount and an async checkout
with a persisted theme; `ShowcaseBrowserIT#stateExampleManagesSharedDerivedAndAsyncState`
validates it in Chromium. The website feature card, tutorial section (heading
15 in both languages) and playground example are wired; `WebsiteBrowserIT`
asserted 11 feature cards and 10 playground examples (13 cards since Sprint 15). See ADR-0020.

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
validates it in Chromium. The website feature card, tutorial section (heading 14 in both languages) and playground example are wired; `WebsiteBrowserIT`
asserted 11 feature cards and 10 playground examples (13 cards since Sprint 15). See ADR-0019.

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
activation-time factory deferral — bundle-level code splitting is not
supported by TeaVM (single bundle) and remains open. The router is not wired to the injector; route components receive
dependencies through plain constructor parameters at the composition root. See
ADR-0017.

Open items carried forward: documented wrong-type-listener failure mode for
outputs, and the absence of an `onMount()` hook (`onInit()` covers
pre-render; anything DOM-effectful must wait for a re-render hook); DI is
still not wired into the component system or the router's outlets.
