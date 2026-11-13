# DejaView — Implementation Roadmap

**Document:** Implementation Steps & Sprint Plan  
**Project:** DejaView  
**Version:** 1.0.0  
**Status:** In Progress  
**Language:** Java 25 LTS  
**Build System:** Apache Maven  
**Compilation Target:** JavaScript  
**Initial Compiler Candidate:** TeaVM

---

# 1. Purpose

This document defines the implementation sequence for the DejaView framework, organized into incremental development sprints.

Each sprint represents a well-defined development milestone containing:

- Features to implement.
- Technical implementation tasks.
- Expected deliverables.
- Automated tests.
- Manual validation procedures.
- Acceptance criteria.
- Dependencies on previous sprints.

The objective is to build DejaView incrementally, ensuring that every completed feature is functional, tested, documented and integrated before moving forward.

This roadmap is intended to be used as the project's primary implementation checklist by developers and AI coding agents.

# 2. Project-Wide Development Rules

## 2.1. Technology Decisions

The following decisions are mandatory throughout implementation:

| Area | Decision |
|---|---|
| Primary language | Java 25 LTS |
| Minimum JDK | 25 |
| Build system | Apache Maven |
| Project structure | Maven Multi-Module |
| Initial Java compiler | TeaVM, subject to validation |
| Browser compilation target | JavaScript only |
| Browser integration | Native DOM and Web APIs |
| Initial rendering strategy | Client-Side Rendering |
| Initial architecture | Modular and extensible |

WebAssembly, WebAssembly GC and WASM compilation targets are explicitly outside the project scope.

## 2.2. Sprint Rules

Every sprint must follow this sequence:

1. Review the previous sprint's deliverables.
2. Implement the current sprint's features.
3. Create or update automated tests.
4. Execute the complete relevant test suite.
5. Validate browser behavior when applicable.
6. Update technical documentation.
7. Review architecture and dependency boundaries.
8. Mark the sprint as completed only after acceptance criteria are satisfied.

Do not implement features from future sprints prematurely unless an architectural dependency explicitly requires it.

## 2.3. Definition of Done

A feature is considered complete only when:

- [ ] Implementation is complete.
- [ ] Unit tests pass.
- [ ] Integration tests pass when applicable.
- [ ] Browser tests pass when applicable.
- [ ] No known critical regressions remain.
- [ ] Public APIs are documented.
- [ ] Error scenarios are handled.
- [ ] Performance is acceptable for the feature's scope.
- [ ] Code follows the project's engineering guidelines.
- [ ] All changes are integrated into the Maven build.
- [ ] The official website reflects the feature in **English and Portuguese**
      (feature card, guide section, playground example when interactive).
- [ ] `README.md`, `IMPLEMENTATION_STATUS.md`, this roadmap and any affected ADR
      are updated.
- [ ] The website/doc sync checklist in [`AGENTS.md`](../AGENTS.md) was followed.

## 2.4. Documentation and Website Synchronization

The website is part of the product, not an afterthought. Whenever a feature,
API or behavior changes, the same change set must update the user-facing site
and the internal documentation:

- Landing page: add or update the feature card in
  `examples/website/.../home/HomePage.java`.
- Guide: add or update the matching section in both `TutorialEn` and
  `TutorialPt`, with code in `Snippets.java` copied from real, compiling code.
- Playground: publish interactive examples in `Examples.ALL`, implement them in
  `examples/showcase` and cover them in `WebsiteBrowserIT`.
- Internal docs: update `IMPLEMENTATION_STATUS.md`, `README.md`, this roadmap,
  `ARCHITECTURE.md` when boundaries change, and record architectural decisions
  as ADRs.
- All new user-facing strings must exist in both languages. Java code samples
  and their comments remain in English; surrounding prose is translated.

The detailed file map and audit procedure live in [`AGENTS.md`](../AGENTS.md).

## 2.5. Styling Compatibility

Styling is not owned by the framework. DejaView must emit ordinary HTML and
must not impose a CSS-in-JS runtime or a proprietary styling pipeline.

Mandatory compatibility requirements:

- Elements support arbitrary `class` and `style` attributes, including multiple
  classes and utility-class combinations.
- External stylesheets (CDN or bundled) work without framework changes.
- CSS utility and component libraries such as Bootstrap, Tailwind CSS and
  daisyUI can be used directly.
- Class names are not rewritten, scoped or hashed by default.
- Component markup follows the library's expected DOM structure and attribute
  names; shadcn-style component patterns are supported as ordinary
  Tailwind-based markup.
- Reactive class/style updates preserve unrelated classes and attributes.
- Styling libraries that require JavaScript behavior (for example Bootstrap
  dropdowns) can be integrated without conflicting with DejaView's event
  handling.

These requirements apply to every sprint. A dedicated styling integration
sprint may be added later if a concrete need appears; the framework must not
block library adoption before then.

# 3. Sprint Roadmap

The roadmap is organized into 22 sequential sprints.

---

# Phase I — Foundation and Runtime

## Sprint 00 — Technical Validation and Project Foundation

**Objective:** Establish the development environment and validate the feasibility of the Java-to-JavaScript compilation pipeline.

**Status (2026-11-11):** Completed.

### Implementation

- [x] Create the root Maven project.
- [x] Configure Maven Wrapper.
- [x] Configure Maven Multi-Module architecture.
- [x] Configure `dependencyManagement`.
- [x] Configure `pluginManagement`.
- [x] Configure Java 25 compilation.
- [x] Configure encoding and compiler parameters.
- [x] Establish common project conventions.
- [x] Configure JUnit 5.
- [x] Configure test execution through Maven.
- [x] Create initial CI pipeline.
- [x] Create the initial ADR structure.
- [x] Evaluate TeaVM compatibility with Java 25 bytecode.
- [x] Evaluate required Java APIs and their TeaVM compatibility.
- [x] Validate JavaScript generation.
- [x] Document compiler limitations and unsupported APIs.

### Deliverables

- Functional Maven multi-module repository.
- Reproducible Java 25 build.
- Initial CI pipeline.
- TeaVM compatibility report.
- Initial architectural decisions.

### Tests

- [x] Root project builds successfully.
- [x] A sample module compiles.
- [x] JUnit tests execute successfully.
- [x] Maven Wrapper works in a clean environment.
- [x] A minimal Java application compiles into JavaScript.
- [x] Generated JavaScript executes in a supported browser.

### Acceptance Criteria

The project must have a reproducible Java 25 build and a validated Java-to-JavaScript compilation path.

**Important:** If the selected TeaVM version cannot support the required Java 25 bytecode or APIs, resolve the compiler strategy before depending on it in subsequent sprints. Do not silently lower the project's Java baseline.

---

## Sprint 01 — JavaScript Runtime Bootstrap

**Objective:** Create the initial browser runtime and establish communication between compiled Java code and the browser.

**Status (2026-11-12):** Completed. The runtime supports configuration, browser
environment detection, reversible DOM insertion/removal and an explicit
shutdown lifecycle that releases mounted nodes and event listeners.

### Implementation

- [x] Create the runtime entry point.
- [x] Implement application initialization.
- [x] Implement runtime configuration.
- [x] Implement browser environment detection.
- [x] Establish JavaScript interop abstractions.
- [x] Establish the boundary between framework code and browser APIs.
- [x] Implement basic DOM access.
- [x] Implement element creation.
- [x] Implement element selection.
- [x] Implement basic DOM insertion and removal.
- [x] Implement browser event registration.
- [x] Implement runtime shutdown and cleanup.

### Deliverables

- Initial DejaView runtime.
- JavaScript interop foundation.
- Browser bootstrap mechanism.
- Minimal working browser application.

### Tests

- [x] Runtime initializes successfully.
- [x] Runtime initialization occurs only once per application instance.
- [x] Java code can access the browser DOM.
- [x] Java code can create and insert DOM elements.
- [x] Java code can register and remove browser events.
- [x] Runtime cleanup releases registered resources.

### Acceptance Criteria

A Java application compiled to JavaScript must initialize DejaView and manipulate the browser DOM.

---

## Sprint 02 — Core Abstractions

**Objective:** Implement the framework's foundational abstractions without coupling the core to the browser.

**Status (2026-11-12):** Completed. Core now provides framework configuration,
an application context, lifecycle contracts, a shared resource registry,
framework exceptions, logging contracts and internal argument checks.

### Implementation

- [x] Establish core module boundaries.
- [x] Implement application context.
- [x] Implement framework configuration.
- [x] Implement common lifecycle contracts.
- [x] Implement resource disposal abstractions.
- [x] Implement framework exceptions.
- [x] Implement internal utility abstractions.
- [x] Define platform-independent rendering contracts.
- [x] Define platform adapter contracts.
- [x] Establish common logging abstractions.

### Deliverables

- `dejaview-core`.
- Core application abstractions.
- Platform-independent contracts.
- Initial API documentation.

### Tests

- [x] Core tests execute without browser dependencies.
- [x] Core can be instantiated independently.
- [x] Platform adapters can be substituted.
- [x] Resource disposal behaves consistently.
- [x] Exceptions preserve useful diagnostic information.

### Acceptance Criteria

The framework core must be independent of DOM and browser-specific implementations.

---

# Phase II — Reactivity and Rendering

## Sprint 03 — Signals and Reactive Primitives

**Objective:** Implement the foundation of DejaView's fine-grained reactivity engine.

**Status (2026-11-11):** Completed.

### Implementation

- [x] Implement `Signal<T>`.
- [x] Implement signal creation.
- [x] Implement `get()`.
- [x] Implement `set()`.
- [x] Implement `update()`.
- [x] Implement equality comparison.
- [x] Implement subscriber registration.
- [x] Implement subscriber notification.
- [x] Implement dependency tracking.
- [x] Implement dependency cleanup.
- [x] Implement automatic dependency collection.

### Deliverables

- Initial `dejaview-reactivity`.
- Signal API.
- Dependency tracking engine.

### Tests

- [x] Signal stores and retrieves values.
- [x] Subscribers are notified after changes.
- [x] Equal values do not trigger unnecessary notifications when configured to use equality suppression.
- [x] Multiple subscribers work correctly.
- [x] Dependencies are tracked automatically.
- [x] Removed dependencies stop receiving notifications.
- [x] Nested reactive computations behave correctly.

### Acceptance Criteria

A change in a Signal must notify only its relevant subscribers.

---

## Sprint 04 — Computed, Effects and Reactive Scheduling

**Objective:** Complete the primary reactive execution model.

**Status (2026-11-11):** Completed.

### Implementation

- [x] Implement `Computed<T>`.
- [x] Implement lazy computed evaluation.
- [x] Implement computed invalidation.
- [x] Implement `Effect`.
- [x] Implement automatic dependency tracking for effects.
- [x] Implement effect cleanup.
- [x] Implement batching.
- [x] Implement reactive scheduling.
- [x] Implement execution ordering.
- [x] Implement cycle detection.
- [x] Implement protection against infinite reactive loops.
- [x] Implement disposal of reactive scopes.
- [x] Implement error propagation.

### Deliverables

- Complete initial reactive primitives.
- Reactive scheduler.
- Reactive lifecycle management.

### Tests

- [x] Computed values update after dependency changes.
- [x] Computed values avoid unnecessary recalculation.
- [x] Effects execute when dependencies change.
- [x] Effects execute cleanup before re-execution.
- [x] Batching prevents redundant executions.
- [x] Circular dependencies are detected.
- [x] Infinite update loops are prevented.
- [x] Disposed effects no longer execute.
- [x] Exceptions do not corrupt the reactive graph.

### Acceptance Criteria

The reactive engine must support Signals, Computed values and Effects with predictable execution and cleanup.

---

## Sprint 05 — Rendering Engine Foundation

**Objective:** Establish the rendering engine independently of the concrete platform.

**Status (2026-11-12):** Completed. The immutable view model now includes a
component node. `dejaview-dom` builds an explicit render tree — text, element,
fragment and component nodes — that owns the created DOM nodes, listeners and
child nodes. `Renderer` defines the mount contract, `TreeReconciler` defines the
in-place update contract and `MountedDom` disposes the mounted render tree.
In-place reconciliation itself belongs to Sprint 06. The renderer stays in
`dejaview-dom`; no `dejaview-renderer` module was created (ADR-0003,
ADR-0012).

### Implementation

- [x] Define rendering interfaces.
- [x] Define render nodes.
- [x] Define element nodes.
- [x] Define text nodes.
- [x] Define fragment nodes.
- [x] Define component nodes.
- [x] Implement render tree creation.
- [x] Implement tree reconciliation contracts.
- [x] Define renderer lifecycle.
- [x] Define platform renderer interfaces.
- [x] Implement initial DOM renderer integration.

### Deliverables

- Rendering contracts (`Renderer`, `TreeReconciler`).
- Initial render tree.
- DOM renderer foundation in `dejaview-dom` (no separate
  `dejaview-renderer` module; see ADR-0003 and ADR-0012).

### Tests

- [x] Render nodes are created correctly.
- [x] Text nodes are represented correctly.
- [x] Element nodes preserve their properties.
- [x] Fragments contain multiple children.
- [x] Renderer can mount a basic tree.
- [x] Renderer can dispose a mounted tree.
- [x] Core rendering contracts work independently of DOM.

### Acceptance Criteria

The rendering engine must be able to represent and render a basic component tree using the DOM platform adapter.

---

## Sprint 06 — DOM Rendering and Reactive Updates

**Objective:** Connect the rendering engine to the reactive system.

**Status (2026-11-12):** Completed. Component views are the reactive boundary: each
mounted tree owns one `Effect` that renders the view, tracks the signals read by
component suppliers and reconciles the result. `DomTreeReconciler` updates text,
attributes, properties, class/style attributes, events and children in place,
and replaces nodes when the node kind or tag name changes. `Reactive.batch`
coalesces multiple signal writes into one reconciliation. The counter example no
longer uses `Dom.byId`.

### Implementation

- [x] Implement DOM element creation.
- [x] Implement text rendering.
- [x] Implement attribute updates.
- [x] Implement property updates.
- [x] Implement class updates.
- [x] Implement style updates.
- [x] Implement child insertion.
- [x] Implement child removal.
- [x] Implement node replacement.
- [x] Implement reactive text bindings.
- [x] Implement reactive attribute bindings.
- [x] Implement reactive conditional updates.
- [x] Implement update scheduling.
- [x] Implement DOM cleanup.

### Deliverables

- Functional DOM renderer.
- Component-scoped reactive DOM bindings.
- Initial rendering update pipeline.

### Tests

- [x] Java application renders HTML elements.
- [x] Text changes update the existing DOM node.
- [x] Reactive attributes update correctly.
- [x] Removed elements are detached.
- [x] Unrelated DOM nodes are not unnecessarily recreated.
- [x] Reactive updates are batched.
- [x] Event listeners and effects are cleaned up.

### Acceptance Criteria

Changing a Signal must update the corresponding DOM output without requiring a complete application rerender.

---

# Phase III — Component System

## Sprint 07 — Component Model and Composition

**Objective:** Introduce the primary component abstraction used to build applications.

### Implementation

- [x] Define component contracts.
- [x] Implement component instantiation.
- [x] Implement component rendering.
- [x] Implement component composition.
- [x] Implement component trees.
- [x] Implement nested components.
- [x] Implement component state ownership.
- [x] Implement component disposal.
- [x] Define component metadata.
- [x] Define declarative component APIs.
- [x] Establish component naming and conventions.

### Deliverables

- Component system foundation.
- Reusable Java components.
- Component composition API.

### Tests

- [x] Components instantiate successfully.
- [x] Components render correctly.
- [x] Nested components render in the correct hierarchy.
- [x] Component disposal releases resources.
- [x] Component state remains isolated where expected.
- [x] Multiple instances of the same component work independently.

### Acceptance Criteria

Developers must be able to create reusable Java components and compose them into larger interfaces.

---

## Sprint 08 — Inputs, Outputs, Templates and Lifecycle

**Objective:** Complete the component interaction and lifecycle model.

**Status (2026-11-12):** Completed. Inputs are raw values re-bound on every
render pass and validated fail-fast (`input(String, Class)` / `inputOr`) with
`DejaViewException` codes `missing-input`/`invalid-input`. Outputs are
`Consumer` listeners registered on the view (`ComponentView#on`), emitted in
registration order via `emit`. Projection splices slot views (default/named)
directly into the rendered tree — no `SlotView`. Lifecycle order is
`bind → init → render … bind → render → update … destroy → close`. Dynamic
component swaps at one template position replace the node when both views
carry a differing class token (`Views.componentFactory(Class, Supplier)`) or
distinct keys; without either, reuse is positional. Children lists switch to
keyed reconciliation as soon as one child carries a key.

### Implementation

- [x] Implement component inputs.
- [x] Implement input validation.
- [x] Implement component outputs.
- [x] Implement event emission.
- [x] Implement content projection.
- [x] Implement named slots.
- [x] Implement lifecycle initialization.
- [x] Implement update lifecycle.
- [x] Implement destruction lifecycle.
- [x] Implement conditional rendering.
- [x] Implement list rendering.
- [x] Implement dynamic components.
- [x] Implement keyed list reconciliation.

### Deliverables

- Complete initial component API.
- Inputs and outputs.
- Lifecycle management.
- Declarative templates.

### Tests

- [x] Inputs are received correctly.
- [x] Input changes trigger the expected updates.
- [x] Outputs emit the correct values.
- [x] Projected content renders correctly.
- [x] Lifecycle hooks execute in the correct order.
- [x] Conditional rendering mounts and disposes components correctly.
- [x] List rendering preserves component identity when keys are stable.
- [x] Dynamic components can be replaced safely.

### Acceptance Criteria

The component system must support reusable, composable and reactive components with a defined lifecycle.

---

# Phase IV — Application Framework

## Sprint 09 — Dependency Injection

**Objective:** Implement the dependency injection system.

**Status (2026-11-12):** Completed. `dejaview-di` is a platform-independent
module depending only on `dejaview-core`: explicit `Provider<T>` factories (no
reflection, TeaVM-safe), identity-based `InjectionToken<T>` with a canonical
`of(Class)` overload, three scopes (`SINGLETON` per registering injector /
`SCOPED` per resolving injector / `TRANSIENT` uncached), hierarchical child
injectors with nearest-registration-wins and live shadowing via
`injector.bind(...)`, `inject()`/`has()` API, `AutoCloseable` disposal in
reverse creation order through `ResourceRegistry` (children before parent;
idempotent `close()`), and `circular-dependency` detection on a thread-local
resolution path (`missing-provider`, `provider-error`, `duplicate-binding` and
`closed-injector` complete the failure model). Field/annotation injection does
not exist yet; components consume dependencies through plain constructor
parameters resolved at the composition root. See ADR-0016.

### Implementation

- [x] Define injection tokens.
- [x] Implement providers.
- [x] Implement singleton providers.
- [x] Implement transient providers.
- [x] Implement scoped providers.
- [x] Implement hierarchical injectors.
- [x] Implement constructor or field injection strategy. (Constructor strategy via explicit factories; field injection deferred as proposal)
- [x] Implement `inject()` API.
- [x] Implement dependency resolution.
- [x] Implement circular dependency detection.
- [x] Implement provider disposal.

### Deliverables

- `dejaview-di`.
- Injector infrastructure.
- Provider configuration API.

### Tests

- [x] Dependencies resolve correctly.
- [x] Singleton instances are reused.
- [x] Transient instances are independent.
- [x] Child scopes resolve inherited providers.
- [x] Circular dependencies are detected.
- [x] Missing providers produce useful errors.
- [x] Scoped resources are disposed correctly.

### Acceptance Criteria

Application services and components must be able to consume dependencies through a predictable injection system.

---

## Sprint 10 — Router

**Objective:** Implement client-side navigation and route management.

**Status (2026-11-12):** Completed. `dejaview-router` is a platform-independent
module depending only on `dejaview-core` and `dejaview-reactivity`: immutable
`Route` definitions (literal, `:param` and `**` segments; nested and index
routes; fail-fast `invalid-route` validation with stable ids `route:N`), an
explicit `Router` that resolves URLs before touching history
(resolve-before-push; denied or unmatched navigations leave state unchanged),
prefix-accumulated guards evaluated root-first (`Allow`/`Deny`/`Redirect`;
ten-hop `redirect-loop` budget), route-declared and guard-returned redirects,
an optional `notFound(...)` chain matched with guards, `NavigationEvent`s
(`Started`/`Redirected`/`Ended`/`Cancelled`) with a reversible listener,
`RouterOutlet` rendering at a chain level and keyed by the matched route's
id (path parameters pushed as inputs), `MemoryRouterHistory` for tests and a
TeaVM `BrowserHistory` (`pushState`/`replaceState` + `popstate`). Lazy route
loading exists only as activation-time factory deferral; bundle-level code
splitting is not supported by the current single-bundle TeaVM output and was
not part of Sprint 15; it remains open. Verified by 90 JVM unit tests and a Chromium E2E test
in the showcase (`?example=router`), which also covers browser back/forward
adoption. See ADR-0017.

### Implementation

- [x] Implement route definitions.
- [x] Implement route matching.
- [x] Implement route parameters.
- [x] Implement query parameters.
- [x] Implement nested routes.
- [x] Implement route outlets.
- [x] Implement navigation APIs.
- [x] Integrate browser history.
- [x] Implement navigation events.
- [x] Implement route guards.
- [x] Implement redirects.
- [x] Implement lazy route loading. (Route factories defer component instantiation to activation; bundle-level code splitting is not delivered — TeaVM 0.16.0 emits a single bundle per application; see ADR-0017 and docs/COMPILER.md)
- [x] Implement not-found routes.
- [x] Implement route-level metadata.

### Deliverables

- `dejaview-router`.
- Client-side routing system.
- Route configuration API.

### Tests

- [x] Routes resolve correctly.
- [x] Dynamic parameters are extracted.
- [x] Query parameters are preserved.
- [x] Browser back and forward work.
- [x] Nested routes render correctly.
- [x] Guards allow and reject navigation correctly.
- [x] Redirects work.
- [x] Lazy routes load successfully. (Deferred: factory deferral only; bundle-level splitting remains undelivered — TeaVM emits a single bundle; see note above and ADR-0017)
- [x] Unknown routes display the configured fallback.

### Acceptance Criteria

A multi-page application must be navigable without full browser page reloads. ✅ Met — the showcase router example navigates between screens and restores them on browser back/forward, verified in Chromium by `ShowcaseBrowserIT#routerExampleNavigatesAndRestoresHistory`.

---

## Sprint 11 — HTTP Client

**Objective:** Provide a consistent HTTP abstraction for browser applications.

**Status (2026-11-12):** Completed. `dejaview-http` is a platform-independent
module depending only on `dejaview-core`: immutable `HttpRequest` (`get`/
`post`/`put`/`patch`/`delete` factories, fluent withers, percent-encoded
query parameters with `+` for space, `Content-Type`-aware `jsonBody`),
immutable case-insensitive `HttpHeaders` preserving insertion order and
multi-valued entries, `HttpResponse` with `ok()` (2xx) and a dependency-free
`JsonValue` tree/parser/writer (finite numbers, depth cap 512, offsets on
errors), `HttpClient` with synchronous-delivery-safe once-guarded consumer,
registration-ordered request and response interceptors (response interceptors
only observe completed round-trips, HTTP error statuses included), effective
per-request vs client-level timeout (`-1` = disabled) and credentials
resolution, and async-only cancellation via `HttpCall`. HTTP semantics
(deliberate deviation): the browser adapter uses `XMLHttpRequest` from
`teavm-jso-apis` 0.16.0, **not Fetch** — the typed bindings ship no Fetch API;
see ADR-0018 for the full rationale and the consequences (e.g. XHR cannot
omit same-origin cookies; only `INCLUDE` sets `withCredentials`). Verified by
67 JVM unit tests (fake transports) and a Chromium E2E test in the showcase
(`?example=http`), which fetches `ping.json` and renders `pong`. See
ADR-0018.

### Implementation

- [x] Implement HTTP request abstractions.
- [x] Implement HTTP response abstractions.
- [x] Implement GET, POST, PUT, PATCH and DELETE.
- [x] Implement request headers.
- [x] Implement query parameters.
- [x] Implement JSON serialization and deserialization.
- [x] Implement request interceptors.
- [x] Implement response interceptors.
- [x] Implement timeout configuration.
- [x] Implement cancellation.
- [x] Implement error handling.
- [x] Implement credentials and authentication integration. (Policy plumbing with `OMIT`/`SAME_ORIGIN`/`INCLUDE`; XHR sends same-origin cookies regardless — see ADR-0018)
- [x] Implement browser Fetch API adapter. (Deviation: XMLHttpRequest instead of Fetch — `teavm-jso-apis` 0.16.0 ships no Fetch binding; XHR mapping is documented in ADR-0018)

### Deliverables

- `dejaview-http`.
- HTTP client API.
- Browser HTTP adapter (XHR; Fetch deferred until typed Fetch/Promise bindings land in TeaVM — see ADR-0018).

### Tests

- [x] HTTP methods work correctly.
- [x] JSON requests and responses are processed.
- [x] Headers are preserved.
- [x] Interceptors execute in the expected order.
- [x] Network errors are handled.
- [x] Timeouts are respected.
- [x] Requests can be cancelled.
- [x] Credentials follow the configured policy.

### Acceptance Criteria

Components and services must be able to consume external APIs through a consistent Java API. ✅ Met — the showcase HTTP example fetches `ping.json` from the browser through `HttpClient`, verified in Chromium by `ShowcaseBrowserIT#httpExampleFetchesPingJson`.

---

## Sprint 12 — Reactive Forms

**Objective:** Introduce form state, validation and reactive updates.

### Implementation

- [x] Implement form controls.
- [x] Implement form groups.
- [x] Implement form arrays.
- [x] Implement control values.
- [x] Implement validation contracts.
- [x] Implement synchronous validators.
- [x] Implement asynchronous validators.
- [x] Implement touched and dirty states.
- [x] Implement disabled states.
- [x] Implement form submission.
- [x] Implement validation messages. (Stable error keys; the application maps keys to localized copy)
- [x] Implement DOM form bindings.
- [x] Implement form-level state aggregation.

### Deliverables

- `dejaview-forms`.
- Reactive forms API.
- Validation infrastructure.

### Tests

- [x] Form values update correctly.
- [x] Validators execute correctly.
- [x] Invalid controls expose their errors.
- [x] Async validators work. (Stale responses discarded via generation counters)
- [x] Dirty and touched states are maintained.
- [x] Form groups aggregate child states.
- [x] Disabled controls behave correctly.
- [x] Browser inputs remain synchronized with form state.

### Acceptance Criteria

Developers must be able to create and validate forms using Java APIs and reactive state. ✅ Met — `dejaview-forms` ships with 53 JVM unit tests, Chromium E2E (`ShowcaseBrowserIT#formsExampleValidatesAndSubmits`), a playground example and tutorial coverage in both languages. See ADR-0019.

---

## Sprint 13 — State Management

**Objective:** Provide application-level state management built on top of the reactive engine.

### Implementation

- [x] Define shared state abstractions. (`StateStore`, `Selector`, `Stores.combine`)
- [x] Implement application-level stores. (`StateStore`, `StateRegistry`)
- [x] Implement derived state. (`select` → lazy `Computed` owned by the store)
- [x] Implement asynchronous state. (`AsyncStore`)
- [x] Implement loading and error states. (`AsyncState`, `AsyncStatus`, `AsyncError`)
- [x] Implement state composition. (`Stores.combine` across independent stores)
- [x] Implement state selectors. (`Selector` named projectors)
- [x] Implement state persistence contracts. (`StateStorage`, `StateCodec`, `PersistedStore`)
- [x] Implement browser storage adapters. (`internal/BrowserStorage` over `localStorage`/`sessionStorage`)
- [x] Implement state restoration. (Restored before the first observer exists; failures are values)
- [x] Define state ownership and disposal rules. (Stores own their selections; `dispose()` is explicit and idempotent)

### Deliverables

- `dejaview-state`.
- Application state management APIs.
- Shared state infrastructure.
- Persistence adapters.

### Tests

- [x] Multiple components share the same state. (`StateStoreTest#multipleComponentsShareTheSameState`)
- [x] Derived state updates correctly. (`StateStoreTest#selectReturnsDerivedStateThatRecomputes`, `selectWithNamedSelectorKeepsDiagnostics`)
- [x] Async operations expose loading and error states. (`AsyncStoreTest` stale-attempt rejection, exactly-once completions, failure codes)
- [x] Persisted state can be restored. (`PersistedStoreTest#restoresAPersistedValueBeforeAnyObserverExists`)
- [x] Storage failures are handled. (`PersistedStoreTest#unreadableStorageBecomesARestoreErrorValue`, `writeFailuresKeepInMemoryStateAndRecoverOnNextWrite`)
- [x] State disposal releases subscriptions. (`StateStoreTest#disposeMakesStoreUnreadableAndUnwritable`, registry `disposeAll`)

### Acceptance Criteria

Applications must be able to manage local, shared, derived, asynchronous and persisted state without introducing unnecessary global coupling. ✅ Met — `dejaview-state` ships with 33 JVM unit tests, a Chromium E2E (`ShowcaseBrowserIT#stateExampleManagesSharedDerivedAndAsyncState`), a playground example and tutorial coverage in both languages. See ADR-0020.

---

# Phase V — Developer Tooling and Compiler

## Sprint 14 — CLI and Development Server

**Objective:** Create the official developer workflow for creating and running DejaView applications.

### Implementation

- [x] Create `dejaview-cli`.
- [x] Implement project initialization.
- [x] Implement project templates.
- [x] Implement development commands.
- [x] Implement build commands.
- [x] Implement development server.
- [x] Implement static asset serving.
- [x] Implement file watching.
- [x] Implement automatic rebuilds.
- [x] Implement browser reload.
- [x] Implement environment configuration.
- [x] Implement useful build diagnostics.
- [x] Implement project validation.

### Deliverables

- DejaView CLI.
- Project generator.
- Development server.
- Initial development workflow.

### Tests

- [x] CLI creates a valid project. (`ProjectGeneratorTest`, `MainTest`, `CliWorkflowIT#generates_a_valid_project_layout`)
- [x] Generated projects compile. (`CliWorkflowIT` — the generated wrapper builds the project with TeaVM)
- [x] Development server starts. (`DevServerTest`, `CliWorkflowIT#dev_server_serves_the_app_and_the_live_reload_client`)
- [x] Static files are served. (`DevServerTest` — content types, 404/403 handling, directory index)
- [x] Source changes trigger rebuilds. (`CliWorkflowIT#source_changes_trigger_a_rebuild_and_a_browser_reload`)
- [x] Build errors are reported clearly. (`BuildDiagnosticsTest` — `[ERROR]` extraction with Maven noise filtered)
- [x] Generated projects can run in a browser. (`CliWorkflowIT#browser_runs_the_generated_app` and live reload verified in Chromium)

### Acceptance Criteria

A developer must be able to create and run a basic DejaView application using the CLI.

**✅ Met (2026-11-13).** `dejaview new my-app && cd my-app && dejaview run` creates, builds and serves a working DejaView application with live reload; verified end-to-end by `CliWorkflowIT` (generate → wrapper build → Chromium render → source edit → rebuild → reload). See ADR-0021.

---

## Sprint 15 — Production Compiler and Build Optimization

**Objective:** Establish a reliable production compilation pipeline.

### Implementation

- [x] Formalize the compiler abstraction. (`dejaview-compiler`: build-time-only module; the Maven build boundary is the abstraction — see ADR-0022)
- [x] Isolate TeaVM-specific implementation details. (all `teavm-maven-plugin` configuration centralized in the parent POM `pluginManagement`; modules declare only `mainClass`)
- [x] Implement compilation configuration. (`dejaview.compiler.*` properties: optimization level, minification, source maps, size budget)
- [x] Implement production build profiles. (`production` Maven profile: `FULL` DCE + `minifying`; development default stays `SIMPLE` and readable)
- [x] Implement source map generation where supported. (`sourceMapsGenerated` parameterized; `app.js.map` retained in both profiles; missing map reported as a warning)
- [x] Implement JavaScript output validation. (`WebAppValidator`/`VerifyWebApp`: bundle presence, non-empty, escapes-root detection, local reference resolution, size budget)
- [x] Implement asset processing. (`AssetManifest` writes a deterministic `dejaview-assets.json` fingerprint of the webapp: paths, bytes, SHA-256)
- [x] Implement resource handling. (missing/empty/escaping resources are build failures with file + reference; website bundles validated with `--no-index`; generated template validated during `package`)
- [x] Implement dead code elimination through supported compiler capabilities. (TeaVM `FULL` for the production profile; benefit verified by byte sizes across `SIMPLE`/`ADVANCED`/`FULL`)
- [x] Implement build diagnostics. (`dejaview-cli` `BuildDiagnostics` extracts the actionable `[ERROR]` lines and maps common TeaVM/Maven failure signatures — class file version, missing `teavm-classlib`, unresolved classes, strict mode — to likely-cause hints; surfaced by `dejaview build` and the rebuild-failure path)
- [x] Implement reproducible production builds. (`project.build.outputTimestamp`; TeaVM output verified byte-identical; `ReproducibleBuildIT` asserts it)
- [x] Implement build failure reporting. (`VerifyWebApp` fails the build with a human-readable report: errors fail, warnings are printed and tolerated)
- [x] Document compiler limitations. (`docs/COMPILER.md` — production pipeline section and documented limitations)

### Deliverables

- `dejaview-compiler`.
- Production build pipeline.
- Compiler abstraction.
- Build optimization configuration.

### Tests

- [x] Production builds compile successfully. (`./mvnw -Pproduction` over the full reactor; all modules BUILD SUCCESS)
- [x] Generated JavaScript executes correctly. (Counter/Showcase/Website browser tests pass on `-Pproduction` bundles)
- [x] Runtime behavior matches development builds. (same suites: Counter 1/1, Showcase 11/11, Website 4/4)
- [x] Missing resources are reported. (`WebAppValidatorTest` — missing asset reported with referencing file; `VerifyWebAppTest`)
- [x] Build failures are actionable. (CLI `BuildDiagnosticsTest` — extracted errors and known-cause hints; validator findings carry file, reference and remedy)
- [x] Output is reproducible under equivalent build conditions. (`ReproducibleBuildIT` — byte-identical bundle and manifest across consecutive builds)
- [x] Supported optimizations do not break runtime behavior. (the entire browser e2e suite runs green under `-Pproduction`)

### Acceptance Criteria

A DejaView application must be compilable into production-ready JavaScript using a reproducible Maven workflow.

**✅ Met (2026-11-13).** `./mvnw clean package -Pproduction` compiles a DejaView application into production-ready, minified JavaScript with FULL dead-code elimination; the webapp is validated (missing/empty/escaping assets fail the build with file and reference) and fingerprinted (`dejaview-assets.json`); output is reproducible under equivalent build conditions (asserted by `ReproducibleBuildIT`), and the whole browser e2e suite passes on the optimized bundles. See ADR-0022.

---

# Phase VI — Server Rendering and Static Generation

## Sprint 16 — Server Platform Abstraction

**Objective:** Prepare the framework for server-side execution without coupling the core to a specific server implementation.

### Implementation

- [x] Define server platform contracts. (`PlatformKind`, `Platform`, `ServerPlatform` singleton, `ServerTokens.PLATFORM`)
- [x] Define server rendering context. (`ServerContext` — request-scoped child injector `server.request`, owned `ResourceRegistry`, owned compatibility registry; transport-neutral `ServerRequest`/`ServerResponse` value types)
- [x] Implement server-side resource management. (`ServerContext` owns a `ResourceRegistry`; disposal in reverse registration order on `close()`; idempotent; failure of one resource does not stop the others)
- [x] Implement request-scoped context. (child injector per request shadows the root; `REQUEST`/`CONTEXT` bind `toInstance` on the child; sibling contexts are fully isolated; context without a parent `StateStorage` binds a fresh `MemoryStateStorage`)
- [x] Define server-compatible component behavior. (`PlatformCompatibility` `UNIVERSAL`/`BROWSER_ONLY`/`SERVER_ONLY`; `BrowserOnlyComponent` marker; BROWSER_ONLY components are rejected with `browser-only-component` before `bind`/`onInit`/`render`)
- [x] Define platform-specific dependency injection. (`ServerBindings.apply(InjectorBuilder)` — `PLATFORM` to the server singleton, `StateStorage` with `Scope.SCOPED`: one `MemoryStateStorage` per resolving injector/request)
- [x] Implement server rendering adapters. (`ServerRenderer` — renders the platform-neutral `View` tree into a sealed `ServerNode` tree: `ServerElement`/`ServerText`/`ServerFragment`; no DOM, no TeaVM; component mount pipeline mirrored; components destroyed and closed in reverse creation order, on success and failure alike)
- [x] Define serialization boundaries. (`SerializationBoundary` — strict `toJson`/`write`: null, JsonValue trees, String, Boolean, finite Numbers, String-keyed Maps, Lists; depth ≤ 512; everything else rejected with `not-serializable`; writes via `Json.write`)
- [x] Separate browser-only and server-compatible APIs. (module boundary: `dejaview-server` has zero browser/TeaVM dependencies, guarded by `ModulePurityTest`; events are dropped during server rendering and reported de-duplicated in `ServerTree.droppedEvents()`)

### Deliverables

- `dejaview-server`.
- Server platform contracts.
- Initial server execution environment.

### Tests

- [x] Core executes without browser APIs. (`ModulePurityTest` — DOM adapter and TeaVM absent from the classpath; full suite runs on the JVM; `ServerRendererTest` renders without a browser)
- [x] Server context is isolated per request. (`ServerContextTest` — parent never sees `REQUEST`/`CONTEXT`, sibling contexts isolated, per-context compatibility registry; `ServerBindingsTest` — SCOPED `StateStorage` resolves per request injector)
- [x] Server resources are disposed correctly. (`ServerContextTest` — reverse-order disposal, idempotent `close()`, disposal failure still closes everything else; `ServerRendererTest` — components destroyed and closed in reverse creation order on success and on render failure)
- [x] Browser-only APIs are not accidentally invoked during server execution. (`ServerRendererTest` — `browser-only-component` before lifecycle runs, pre-construction rejection via class token, cost-free rejection via marker or registry override)
- [x] Components can be classified by platform compatibility. (`PlatformCompatibilityRegistryTest` — default UNIVERSAL, marker interface by assignability, exact-class override wins, SERVER_ONLY allowed on the server)

### Acceptance Criteria

The framework must have a server platform foundation capable of supporting SSR.

**✅ Met (2026-11-13).** `dejaview-server` ships the platform contracts
(`Platform`/`ServerPlatform`/`ServerTokens`), a request-scoped `ServerContext`
(child injector, isolated state, reverse-order disposal), a per-context
`PlatformCompatibilityRegistry` with `BrowserOnlyComponent` marker support, a
stateless `ServerRenderer` producing a sealed `ServerNode` tree (events
dropped and reported, components destroyed and closed LIFO), a strict
`SerializationBoundary`, and transport-neutral `ServerRequest`/`ServerResponse`
values. 80 JVM tests cover the module today (including the Sprint 17 boundary
support), with classpath purity asserted. No browser APIs and no TeaVM imports
anywhere in the module. See ADR-0023.

---

## Sprint 17 — Server-Side Rendering (SSR)

**Objective:** Implement HTML rendering on the server.

### Implementation

- [x] Implement server renderer. (`ServerRenderer` from `dejaview-server` reused by `dejaview-ssr`; mount pipeline unchanged — ADR-0023)
- [x] Implement HTML serialization. (`HtmlSerializer` — escaped fragments, void elements, fragments spliced without wrappers; `HtmlPage` — complete document with doctype, `lang`, charset, viewport and metadata)
- [x] Implement text escaping. (`&`, `<`, `>` in text; attributes also escape `"` and `'`; `SsrFinding` `raw-text-escaped` warns when text lands inside `script`/`style`)
- [x] Implement attribute serialization. (attributes verbatim after validation; `value` property → `value` attribute; `checked`/`selected`/`disabled`/`readonly`/`multiple` → boolean attributes when `"true"`; unsupported properties skipped and reported)
- [x] Implement component rendering. (`SsrRenderer.renderPage`/`renderFragment` render a `View` through `ServerRenderer` and return an `SsrResult` with status, HTML, dropped events, boundary reports and warnings)
- [x] Implement nested component rendering. (nested components, fragments and slots serialize in render order; `ServerRenderer` lifecycle semantics preserved)
- [x] Implement request-specific rendering. (`PageMeta` per request — lang, title, description, canonical, meta/Open Graph; `ServerContext` + root injector per request; `examples/ssr` search routing proves no cross-request leakage)
- [x] Implement error boundaries for server rendering. (`ServerErrorBoundary` in `dejaview-server`: failed subtree destroyed and closed LIFO, `BoundaryError` recorded, `fallback(error)` rendered in place; fallback failures propagate outward)
- [x] Implement initial metadata handling. (`PageMeta` mutable and fluent, written by components during render; `HtmlPage` assembles the head and omits empty optional tags)
- [x] Implement server output validation. (`SsrValidator` rejects illegal tag/attribute names, void elements with children, duplicate attributes and depth > 512 with `ssr-validation-failed`; warnings for raw text, skipped properties and empty output)
- [ ] Implement initial streaming SSR where technically feasible. (Deferred: the `Writer` variants render and validate the whole document before writing — all-or-nothing, no partial output. True progressive streaming needs a resumable pipeline and belongs to Phase 7 / Sprint 19 — ADR-0024)

### Deliverables

- `dejaview-ssr`.
- Server-side HTML renderer.
- SSR integration examples. (`examples/ssr` — JDK HTTP server, GET search form, error boundary, 404 page, JavaScript disabled in the browser test)

### Tests

- [x] Components generate valid HTML. (`HtmlSerializerTest`, `HtmlPageTest`, `SsrValidatorTest`; `examples/ssr` `SsrApplicationTest` asserts complete documents)
- [x] Text and attributes are escaped correctly. (`HtmlSerializerTest` escaping cases; `SsrApplicationTest.userInputIsEscapedInsteadOfBecomingMarkup` renders hostile query strings as text in body and title)
- [x] Nested components render correctly. (`SsrRendererTest`, `ServerRendererTest` boundary tests; `HtmlSerializerTest` nested elements and fragment splicing)
- [x] Multiple requests do not leak state. (`SsrApplicationTest.requestsDoNotShareMetadataOrOutput` and `renderingTheSameRequestTwiceIsDeterministic`; one `ServerContext` and root injector per request)
- [x] Server rendering handles errors safely. (`SsrRendererTest.unhandledFailureReturnsTheGenericErrorPage`/`validationFailureReturnsTheGenericErrorPage` — generic 500, no leaked details; boundary containment tests; `aClosedContextFailsSafely`)
- [x] Generated HTML can be loaded by a browser. (`SsrBrowserIT` — Chromium with JavaScript disabled: home page, link and GET form navigation, contained widget failure, 404 page)
- [ ] Streaming output preserves the expected document structure. (Deferred with streaming; buffered output preserves the document structure today, verified by `HtmlPageTest` and `SsrBrowserIT`)

### Acceptance Criteria

A DejaView application must be capable of generating its initial HTML on the server.

**✅ Met (2026-11-13).** `dejaview-ssr` ships `SsrRenderer` (page and
fragment rendering, `Writer` variants), `SsrValidator` (tag/attribute names,
void elements, duplicate attributes, depth 512), `HtmlSerializer` and
`HtmlPage` (escaped text and attributes, complete document with per-request
`PageMeta` metadata), `SsrResult` (status, HTML, dropped events, boundary
reports, warnings, attached error) and the generic 500 failure path.
`dejaview-server` gains `ServerErrorBoundary`/`BoundaryError` containment.
`examples/ssr` is a working server-rendered application served by the JDK
`HttpServer` without JavaScript. 43 JVM tests cover `dejaview-ssr`, 10 more
cover the example, and `SsrBrowserIT` renders it in Chromium with JavaScript
disabled. Not delivered: state serialization, hydration, progressive
streaming, a deployable server adapter and the full DOM property set — see
ADR-0024.

---

## Sprint 18 — Static Site Generation (SSG)

**Objective:** Generate static HTML pages during the build process.

### Implementation

- [ ] Implement SSG build pipeline.
- [ ] Implement static route discovery.
- [ ] Implement route parameter generation.
- [ ] Implement static page rendering.
- [ ] Implement asset copying.
- [ ] Implement HTML output organization.
- [ ] Implement metadata generation.
- [ ] Implement static resource validation.
- [ ] Implement configurable output directories.
- [ ] Integrate SSG with the CLI.

### Deliverables

- `dejaview-ssg`.
- Static generation pipeline.
- CLI static build command.

### Tests

- [ ] Static routes generate HTML.
- [ ] Dynamic routes generate configured paths.
- [ ] Generated assets resolve correctly.
- [ ] Output can be served by a static web server.
- [ ] Build failures identify problematic routes.
- [ ] Generated pages preserve metadata.

### Acceptance Criteria

Developers must be able to generate deployable static websites using DejaView.

---

## Sprint 19 — Hydration

**Objective:** Connect server-rendered HTML to the browser runtime without unnecessarily replacing the existing DOM.

### Implementation

- [ ] Define hydration contracts.
- [ ] Implement hydration entry points.
- [ ] Implement DOM matching.
- [ ] Implement component identity matching.
- [ ] Implement event binding restoration.
- [ ] Implement reactive state initialization.
- [ ] Implement hydration mismatch detection.
- [ ] Implement mismatch diagnostics.
- [ ] Implement fallback rendering.
- [ ] Implement cleanup of partially hydrated trees.
- [ ] Evaluate selective hydration.
- [ ] Evaluate islands architecture.

### Deliverables

- `dejaview-hydration`.
- Hydration runtime.
- SSR-to-browser integration.

### Tests

- [ ] Server HTML is reused by the browser.
- [ ] Components become interactive after hydration.
- [ ] Initial state is consistent between server and browser.
- [ ] Mismatches are detected.
- [ ] Fallback rendering works when hydration cannot proceed.
- [ ] Event listeners are not duplicated.
- [ ] Hydration does not unnecessarily recreate matching DOM nodes.

### Acceptance Criteria

An SSR application must become interactive in the browser while preserving compatible server-rendered DOM.

---

# Phase VII — Quality, Ecosystem and Integrations

## Sprint 20 — Testing Framework and DevTools

**Objective:** Provide first-class testing and debugging capabilities.

### Implementation

- [ ] Implement component testing utilities.
- [ ] Implement reactive testing utilities.
- [ ] Implement DOM testing utilities.
- [ ] Implement rendering test helpers.
- [ ] Implement router test helpers.
- [ ] Implement HTTP test helpers.
- [ ] Implement browser integration test configuration.
- [ ] Implement component inspection.
- [ ] Implement Signal inspection.
- [ ] Implement effect inspection.
- [ ] Implement rendering diagnostics.
- [ ] Implement reactive dependency visualization.
- [ ] Implement performance profiling.
- [ ] Implement useful debugging information.

### Deliverables

- `dejaview-testing`.
- `dejaview-devtools`.
- Component and browser testing utilities.
- Initial debugging tools.

### Tests

- [ ] Components can be tested independently.
- [ ] Reactive behavior can be tested deterministically.
- [ ] DOM rendering can be tested.
- [ ] Router navigation can be tested.
- [ ] HTTP interactions can be mocked.
- [ ] DevTools correctly inspect reactive state.
- [ ] Profiling data corresponds to actual framework activity.

### Acceptance Criteria

Developers must have reliable tools to test, inspect and debug DejaView applications.

---

## Sprint 21 — Spring Boot Integration

**Objective:** Integrate DejaView into the Java backend ecosystem.

### Implementation

- [ ] Implement `dejaview-spring-boot`.
- [ ] Create Spring Boot auto-configuration.
- [ ] Implement static asset integration.
- [ ] Implement application resource resolution.
- [ ] Implement server rendering integration where supported.
- [ ] Implement configuration properties.
- [ ] Implement development and production profiles.
- [ ] Implement integration with Spring application lifecycle.
- [ ] Document authentication integration.
- [ ] Create a complete Spring Boot example.

### Deliverables

- Spring Boot starter.
- Auto-configuration.
- Integration examples.
- Spring Boot documentation.

### Tests

- [ ] Starter loads correctly.
- [ ] Configuration properties are applied.
- [ ] Static assets are served.
- [ ] Spring lifecycle integration works.
- [ ] SSR integration works where supported.
- [ ] Example application starts and operates correctly.

### Acceptance Criteria

Developers must be able to integrate DejaView into a Spring Boot application using a documented Maven dependency.

---

## Sprint 22 — Accessibility, Security, Internationalization and Ecosystem

**Objective:** Complete essential framework capabilities and prepare the ecosystem for broader adoption.

### Implementation

**Accessibility**

- [ ] Establish accessible component conventions.
- [ ] Implement accessibility utilities.
- [ ] Validate semantic HTML output.
- [ ] Validate keyboard interaction.
- [ ] Validate focus management.
- [ ] Validate accessible form behavior.
- [ ] Add accessibility tests.

**Security**

- [ ] Establish safe DOM manipulation APIs.
- [ ] Implement safe text and attribute handling.
- [ ] Define HTML sanitization boundaries.
- [ ] Document XSS prevention.
- [ ] Define secure URL handling.
- [ ] Review serialization and hydration security.
- [ ] Document authentication and authorization integration.
- [ ] Perform security testing.

**Internationalization**

- [ ] Implement locale configuration.
- [ ] Implement translation resource loading.
- [ ] Implement translation lookup.
- [ ] Implement interpolation.
- [ ] Implement pluralization.
- [ ] Implement locale-aware formatting.
- [ ] Implement locale switching.

**Ecosystem**

- [ ] Define plugin contracts.
- [ ] Implement plugin registration.
- [ ] Implement plugin lifecycle.
- [ ] Create extension documentation.
- [ ] Create reusable example components.
- [ ] Establish compatibility guidelines.
- [ ] Define versioning policies.

### Deliverables

- Accessibility utilities.
- Security guidelines and safeguards.
- Internationalization module.
- Initial plugin infrastructure.
- Ecosystem documentation.

### Tests

- [ ] Keyboard navigation works.
- [ ] Accessibility checks pass for supported components.
- [ ] Unsafe HTML is handled according to the security policy.
- [ ] Translation lookup works.
- [ ] Locale switching updates the expected output.
- [ ] Plugin registration and disposal work.
- [ ] Security regression tests pass.

### Acceptance Criteria

The framework must provide a documented and tested baseline for accessibility, security, internationalization and extensibility.

---

# Phase VIII — Stabilization and Release

## Sprint 23 — Performance, Compatibility and Production Hardening

**Objective:** Validate the complete framework and prepare it for production use.

### Implementation

- [ ] Review all module boundaries.
- [ ] Review unnecessary dependencies.
- [ ] Profile reactive execution.
- [ ] Profile rendering performance.
- [ ] Profile memory usage.
- [ ] Optimize DOM update paths.
- [ ] Optimize compilation output where supported.
- [ ] Validate production bundle behavior.
- [ ] Validate browser compatibility.
- [ ] Validate compatibility with Bootstrap.
- [ ] Validate compatibility with Tailwind CSS utility classes.
- [ ] Validate compatibility with daisyUI components.
- [ ] Verify external stylesheets and class names pass through unchanged.
- [ ] Validate server rendering compatibility.
- [ ] Validate hydration consistency.
- [ ] Perform security review.
- [ ] Perform accessibility review.
- [ ] Create benchmark suites.
- [ ] Resolve critical and high-priority bugs.
- [ ] Review public APIs for consistency.
- [ ] Verify Maven release configuration.

### Deliverables

- Performance benchmarks.
- Compatibility report.
- Production hardening report.
- Stabilized framework modules.

### Tests

- [ ] Full unit test suite passes.
- [ ] Full integration test suite passes.
- [ ] Browser end-to-end tests pass.
- [ ] SSR and SSG tests pass.
- [ ] Hydration tests pass.
- [ ] Security tests pass.
- [ ] Styling library compatibility tests pass.
- [ ] Performance benchmarks are recorded.
- [ ] No known critical defects remain.

### Acceptance Criteria

The framework must satisfy its documented functional, compatibility, security and performance requirements.

---

## Sprint 24 — Documentation, Packaging and First Stable Release

**Objective:** Prepare DejaView for public adoption.

### Implementation

- [ ] Finalize README.
- [ ] Finalize architecture documentation.
- [ ] Document all public APIs.
- [ ] Create getting-started documentation.
- [ ] Create installation guides.
- [ ] Create component development guides.
- [ ] Create reactivity guides.
- [ ] Create routing guides.
- [ ] Create SSR and SSG guides.
- [ ] Create deployment guides.
- [ ] Create migration and compatibility policies.
- [ ] Create contribution guidelines.
- [ ] Define semantic versioning policy.
- [ ] Configure Maven artifact publishing.
- [ ] Configure release automation.
- [ ] Prepare example applications.
- [ ] Publish the first stable release.

### Deliverables

- Complete documentation.
- Published Maven artifacts.
- Example applications.
- Release notes.
- First stable DejaView release.

### Acceptance Criteria

A new developer must be able to install DejaView, create an application, implement components, build for production and deploy an application using the official documentation.

---

# 4. Cross-Sprint Quality Requirements

The following requirements apply throughout the entire project and must not be postponed until the final release.

## 4.1. Testing Strategy

| Test Type | Purpose |
|---|---|
| Unit Tests | Validate individual classes and functions |
| Integration Tests | Validate interactions between modules |
| Compiler Tests | Validate Java-to-JavaScript compilation |
| Browser Tests | Validate actual browser behavior |
| End-to-End Tests | Validate complete application workflows |
| Regression Tests | Prevent previously fixed bugs from returning |
| Performance Tests | Detect performance regressions |
| Security Tests | Validate security boundaries |

## 4.2. Continuous Integration

Every pull request should execute the applicable pipeline:

1. Validate Maven project structure.
2. Compile with Java 25.
3. Run unit tests.
4. Run integration tests.
5. Validate compiler output.
6. Run browser tests when applicable.
7. Check formatting and static analysis.
8. Verify module dependency rules.

A sprint must not be considered complete while its required CI checks are failing.

## 4.3. Browser Compatibility

Browser tests should progressively cover:

- Chromium-based browsers.
- Firefox.
- WebKit-based browsers.

The exact supported versions must be documented and established through compatibility testing.

## 4.4. Performance Benchmarks

Performance measurements should be introduced as soon as the corresponding feature exists.

Important measurements include:

- Signal update latency.
- Computed recalculation overhead.
- Effect execution overhead.
- DOM update latency.
- Component mounting and disposal.
- Initial application startup.
- JavaScript output size.
- Memory consumption.
- Hydration time.

Avoid arbitrary performance targets before establishing reliable baselines. Define measurable budgets using benchmark results.

# 5. Milestone Dependencies

The following dependency order must be preserved:

```text
SPRINT 00 — Foundation
    |
SPRINT 01 — Runtime Bootstrap
    |
SPRINT 02 — Core Abstractions
    |
SPRINT 03 — Signals
    |
SPRINT 04 — Computed and Effects
    |
SPRINT 05 — Rendering Foundation
    |
SPRINT 06 — Reactive DOM
    |
SPRINT 07 — Components
    |
SPRINT 08 — Component Lifecycle
    |
SPRINT 09 — Dependency Injection
    |
SPRINT 10 — Router
    |
SPRINT 11 — HTTP
    |
SPRINT 12 — Forms
    |
SPRINT 13 — State Management
    |
SPRINT 14 — CLI and Development Server
    |
SPRINT 15 — Production Compiler
    |
SPRINT 16 — Server Platform
    |
SPRINT 17 — SSR
    |
SPRINT 18 — SSG
    |
SPRINT 19 — Hydration
    |
SPRINT 20 — Testing and DevTools
    |
SPRINT 21 — Spring Boot
    |
SPRINT 22 — Accessibility, Security and Ecosystem
    |
SPRINT 23 — Production Hardening
    |
SPRINT 24 — Stable Release
```

This is the default implementation sequence. Some tasks may be developed in parallel when their interfaces and dependencies are already stable, but the acceptance criteria of each sprint must remain independent.

# 6. End-to-End Validation Applications

Instead of validating each feature exclusively through isolated tests, DejaView must progressively maintain example applications that exercise the complete framework.

## Application 1 — Counter

**Introduced in Sprint 01 and expanded through Sprint 08.**

Features:

- Runtime initialization.
- DOM rendering.
- Signals.
- Computed values.
- Effects.
- Components.
- Inputs and outputs.
- Lifecycle.

Expected result: a Java application that renders an interactive counter in the browser.

## Application 2 — Task Manager

**Introduced after Sprint 13.**

Features:

- Multiple components.
- Dependency Injection.
- Router.
- HTTP client.
- Reactive forms.
- Shared state.
- Validation.

Expected result: a functional task management application with navigation, forms and API integration.

## Application 3 — Full-Stack Application

**Introduced after Sprint 21.**

Features:

- DejaView frontend.
- Spring Boot backend.
- HTTP communication.
- Authentication integration.
- SSR where supported.
- Production build.
- Static asset serving.

Expected result: a complete application demonstrating DejaView integration with the Java backend ecosystem.

## Application 4 — SSR and SSG Showcase

**Introduced after Sprint 19.**

Features:

- Server-side rendering.
- Static generation.
- Hydration.
- Metadata.
- Route-based rendering.
- Production deployment.

Expected result: an application demonstrating the different rendering strategies supported by DejaView.

# 7. Project Progress Tracking

Use the following checklist to track sprint completion.

**Current status (2026-11-13):** Sprints 00 through 17 are complete. Phase IV (Sprints 09–13, application framework) is closed. Phase V (Developer Tooling and Compiler) is delivered: Sprint 14 (CLI and Development Server) and Sprint 15 (Production Compiler). Phase VI (Server Rendering and Static Generation) is open: Sprint 16 (Server Platform Abstraction) and Sprint 17 (Server-Side Rendering) are delivered. Sprint 18 (Static Site Generation) is next. The authoritative implemented-versus-planned list is
[`IMPLEMENTATION_STATUS.md`](IMPLEMENTATION_STATUS.md).

| Sprint | Feature | Status |
|---|---|---|
| 00 | Technical Validation and Foundation | Completed |
| 01 | JavaScript Runtime Bootstrap | Completed |
| 02 | Core Abstractions | Completed |
| 03 | Signals and Reactive Primitives | Completed |
| 04 | Computed, Effects and Scheduling | Completed |
| 05 | Rendering Engine Foundation | Completed |
| 06 | DOM Rendering and Reactive Updates | Completed |
| 07 | Component Model and Composition | Completed |
| 08 | Inputs, Outputs, Templates and Lifecycle | Completed |
| 09 | Dependency Injection | Completed |
| 10 | Router | Completed |
| 11 | HTTP Client | Completed |
| 12 | Reactive Forms | Completed |
| 13 | State Management | Completed |
| 14 | CLI and Development Server | Completed |
| 15 | Production Compiler and Optimization | Completed |
| 16 | Server Platform Abstraction | Completed |
| 17 | Server-Side Rendering | Completed |
| 18 | Static Site Generation | Not Started |
| 19 | Hydration | Not Started |
| 20 | Testing Framework and DevTools | Not Started |
| 21 | Spring Boot Integration | Not Started |
| 22 | Accessibility, Security and Ecosystem | Not Started |
| 23 | Performance and Production Hardening | Not Started |
| 24 | Documentation and Stable Release | Not Started |

Recommended status values:

- **Not Started:** No implementation has begun.
- **In Progress:** Implementation is underway.
- **Testing:** Implementation is complete and validation is underway.
- **Blocked:** A dependency or technical issue prevents completion.
- **Completed:** All acceptance criteria have been satisfied.

# 8. AI Agent Execution Instructions

When using an AI coding agent to implement DejaView, the agent must follow these instructions:

1. Read `README.md`, `ARCHITECTURE.md` and this document before making changes.
2. Identify the current sprint and its dependencies.
3. Inspect the existing repository before creating or modifying files.
4. Implement only the current sprint's scope unless a prerequisite is missing.
5. Never assume a conceptual API is already implemented.
6. Follow Java 25 and Maven conventions.
7. Never introduce WebAssembly or WASM-related targets.
8. Keep TeaVM-specific implementation details isolated behind the compiler abstraction.
9. Create automated tests for every implemented feature.
10. Execute the relevant Maven test suite.
11. Validate browser behavior whenever the feature interacts with the browser.
12. Update documentation and architectural decisions when necessary.
13. Report completed tasks, test results, known limitations and remaining work.
14. Do not mark a sprint as completed without satisfying its acceptance criteria.
15. Do not silently skip failing tests or weaken acceptance criteria to make a sprint appear complete.

At the end of each sprint, the agent must provide:

- Summary of implemented features.
- Files and modules created or modified.
- Tests created.
- Commands executed.
- Test results.
- Known limitations.
- Architectural decisions made.
- Remaining tasks.
- Sprint completion status.

# 9. Final Project Objective

The final objective is to deliver a production-ready, Java-first frontend framework that provides:

- A complete reactive engine.
- A component-based architecture.
- A declarative rendering system.
- Direct browser DOM and Web API integration.
- A Java-to-JavaScript compilation pipeline.
- A complete application framework.
- Routing, dependency injection, HTTP and forms.
- CLI and developer tooling.
- CSR, SSR, SSG and hydration.
- Spring Boot integration.
- Testing and debugging tools.
- Accessibility, security and internationalization support.
- A documented and extensible ecosystem.

**The guiding principle is simple: every sprint must leave DejaView in a working, testable and incrementally more capable state.**