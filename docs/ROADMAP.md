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

- [ ] Define component contracts.
- [ ] Implement component instantiation.
- [ ] Implement component rendering.
- [ ] Implement component composition.
- [ ] Implement component trees.
- [ ] Implement nested components.
- [ ] Implement component state ownership.
- [ ] Implement component disposal.
- [ ] Define component metadata.
- [ ] Define declarative component APIs.
- [ ] Establish component naming and conventions.

### Deliverables

- Component system foundation.
- Reusable Java components.
- Component composition API.

### Tests

- [ ] Components instantiate successfully.
- [ ] Components render correctly.
- [ ] Nested components render in the correct hierarchy.
- [ ] Component disposal releases resources.
- [ ] Component state remains isolated where expected.
- [ ] Multiple instances of the same component work independently.

### Acceptance Criteria

Developers must be able to create reusable Java components and compose them into larger interfaces.

---

## Sprint 08 — Inputs, Outputs, Templates and Lifecycle

**Objective:** Complete the component interaction and lifecycle model.

### Implementation

- [ ] Implement component inputs.
- [ ] Implement input validation.
- [ ] Implement component outputs.
- [ ] Implement event emission.
- [ ] Implement content projection.
- [ ] Implement named slots.
- [ ] Implement lifecycle initialization.
- [ ] Implement update lifecycle.
- [ ] Implement destruction lifecycle.
- [ ] Implement conditional rendering.
- [ ] Implement list rendering.
- [ ] Implement dynamic components.
- [ ] Implement keyed list reconciliation.

### Deliverables

- Complete initial component API.
- Inputs and outputs.
- Lifecycle management.
- Declarative templates.

### Tests

- [ ] Inputs are received correctly.
- [ ] Input changes trigger the expected updates.
- [ ] Outputs emit the correct values.
- [ ] Projected content renders correctly.
- [ ] Lifecycle hooks execute in the correct order.
- [ ] Conditional rendering mounts and disposes components correctly.
- [ ] List rendering preserves component identity when keys are stable.
- [ ] Dynamic components can be replaced safely.

### Acceptance Criteria

The component system must support reusable, composable and reactive components with a defined lifecycle.

---

# Phase IV — Application Framework

## Sprint 09 — Dependency Injection

**Objective:** Implement the dependency injection system.

### Implementation

- [ ] Define injection tokens.
- [ ] Implement providers.
- [ ] Implement singleton providers.
- [ ] Implement transient providers.
- [ ] Implement scoped providers.
- [ ] Implement hierarchical injectors.
- [ ] Implement constructor or field injection strategy.
- [ ] Implement `inject()` API.
- [ ] Implement dependency resolution.
- [ ] Implement circular dependency detection.
- [ ] Implement provider disposal.

### Deliverables

- `dejaview-di`.
- Injector infrastructure.
- Provider configuration API.

### Tests

- [ ] Dependencies resolve correctly.
- [ ] Singleton instances are reused.
- [ ] Transient instances are independent.
- [ ] Child scopes resolve inherited providers.
- [ ] Circular dependencies are detected.
- [ ] Missing providers produce useful errors.
- [ ] Scoped resources are disposed correctly.

### Acceptance Criteria

Application services and components must be able to consume dependencies through a predictable injection system.

---

## Sprint 10 — Router

**Objective:** Implement client-side navigation and route management.

### Implementation

- [ ] Implement route definitions.
- [ ] Implement route matching.
- [ ] Implement route parameters.
- [ ] Implement query parameters.
- [ ] Implement nested routes.
- [ ] Implement route outlets.
- [ ] Implement navigation APIs.
- [ ] Integrate browser history.
- [ ] Implement navigation events.
- [ ] Implement route guards.
- [ ] Implement redirects.
- [ ] Implement lazy route loading.
- [ ] Implement not-found routes.
- [ ] Implement route-level metadata.

### Deliverables

- `dejaview-router`.
- Client-side routing system.
- Route configuration API.

### Tests

- [ ] Routes resolve correctly.
- [ ] Dynamic parameters are extracted.
- [ ] Query parameters are preserved.
- [ ] Browser back and forward work.
- [ ] Nested routes render correctly.
- [ ] Guards allow and reject navigation correctly.
- [ ] Redirects work.
- [ ] Lazy routes load successfully.
- [ ] Unknown routes display the configured fallback.

### Acceptance Criteria

A multi-page application must be navigable without full browser page reloads.

---

## Sprint 11 — HTTP Client

**Objective:** Provide a consistent HTTP abstraction for browser applications.

### Implementation

- [ ] Implement HTTP request abstractions.
- [ ] Implement HTTP response abstractions.
- [ ] Implement GET, POST, PUT, PATCH and DELETE.
- [ ] Implement request headers.
- [ ] Implement query parameters.
- [ ] Implement JSON serialization and deserialization.
- [ ] Implement request interceptors.
- [ ] Implement response interceptors.
- [ ] Implement timeout configuration.
- [ ] Implement cancellation.
- [ ] Implement error handling.
- [ ] Implement credentials and authentication integration.
- [ ] Implement browser Fetch API adapter.

### Deliverables

- `dejaview-http`.
- HTTP client API.
- Browser Fetch adapter.

### Tests

- [ ] HTTP methods work correctly.
- [ ] JSON requests and responses are processed.
- [ ] Headers are preserved.
- [ ] Interceptors execute in the expected order.
- [ ] Network errors are handled.
- [ ] Timeouts are respected.
- [ ] Requests can be cancelled.
- [ ] Credentials follow the configured policy.

### Acceptance Criteria

Components and services must be able to consume external APIs through a consistent Java API.

---

## Sprint 12 — Reactive Forms

**Objective:** Introduce form state, validation and reactive updates.

### Implementation

- [ ] Implement form controls.
- [ ] Implement form groups.
- [ ] Implement form arrays.
- [ ] Implement control values.
- [ ] Implement validation contracts.
- [ ] Implement synchronous validators.
- [ ] Implement asynchronous validators.
- [ ] Implement touched and dirty states.
- [ ] Implement disabled states.
- [ ] Implement form submission.
- [ ] Implement validation messages.
- [ ] Implement DOM form bindings.
- [ ] Implement form-level state aggregation.

### Deliverables

- `dejaview-forms`.
- Reactive forms API.
- Validation infrastructure.

### Tests

- [ ] Form values update correctly.
- [ ] Validators execute correctly.
- [ ] Invalid controls expose their errors.
- [ ] Async validators work.
- [ ] Dirty and touched states are maintained.
- [ ] Form groups aggregate child states.
- [ ] Disabled controls behave correctly.
- [ ] Browser inputs remain synchronized with form state.

### Acceptance Criteria

Developers must be able to create and validate forms using Java APIs and reactive state.

---

## Sprint 13 — State Management

**Objective:** Provide application-level state management built on top of the reactive engine.

### Implementation

- [ ] Define shared state abstractions.
- [ ] Implement application-level stores.
- [ ] Implement derived state.
- [ ] Implement asynchronous state.
- [ ] Implement loading and error states.
- [ ] Implement state composition.
- [ ] Implement state selectors.
- [ ] Implement state persistence contracts.
- [ ] Implement browser storage adapters.
- [ ] Implement state restoration.
- [ ] Define state ownership and disposal rules.

### Deliverables

- Application state management APIs.
- Shared state infrastructure.
- Persistence adapters.

### Tests

- [ ] Multiple components share the same state.
- [ ] Derived state updates correctly.
- [ ] Async operations expose loading and error states.
- [ ] Persisted state can be restored.
- [ ] Storage failures are handled.
- [ ] State disposal releases subscriptions.

### Acceptance Criteria

Applications must be able to manage local, shared, derived, asynchronous and persisted state without introducing unnecessary global coupling.

---

# Phase V — Developer Tooling and Compiler

## Sprint 14 — CLI and Development Server

**Objective:** Create the official developer workflow for creating and running DejaView applications.

### Implementation

- [ ] Create `dejaview-cli`.
- [ ] Implement project initialization.
- [ ] Implement project templates.
- [ ] Implement development commands.
- [ ] Implement build commands.
- [ ] Implement development server.
- [ ] Implement static asset serving.
- [ ] Implement file watching.
- [ ] Implement automatic rebuilds.
- [ ] Implement browser reload.
- [ ] Implement environment configuration.
- [ ] Implement useful build diagnostics.
- [ ] Implement project validation.

### Deliverables

- DejaView CLI.
- Project generator.
- Development server.
- Initial development workflow.

### Tests

- [ ] CLI creates a valid project.
- [ ] Generated projects compile.
- [ ] Development server starts.
- [ ] Static files are served.
- [ ] Source changes trigger rebuilds.
- [ ] Build errors are reported clearly.
- [ ] Generated projects can run in a browser.

### Acceptance Criteria

A developer must be able to create and run a basic DejaView application using the CLI.

---

## Sprint 15 — Production Compiler and Build Optimization

**Objective:** Establish a reliable production compilation pipeline.

### Implementation

- [ ] Formalize the compiler abstraction.
- [ ] Isolate TeaVM-specific implementation details.
- [ ] Implement compilation configuration.
- [ ] Implement production build profiles.
- [ ] Implement source map generation where supported.
- [ ] Implement JavaScript output validation.
- [ ] Implement asset processing.
- [ ] Implement resource handling.
- [ ] Implement dead code elimination through supported compiler capabilities.
- [ ] Implement build diagnostics.
- [ ] Implement reproducible production builds.
- [ ] Implement build failure reporting.
- [ ] Document compiler limitations.

### Deliverables

- `dejaview-compiler`.
- Production build pipeline.
- Compiler abstraction.
- Build optimization configuration.

### Tests

- [ ] Production builds compile successfully.
- [ ] Generated JavaScript executes correctly.
- [ ] Runtime behavior matches development builds.
- [ ] Missing resources are reported.
- [ ] Build failures are actionable.
- [ ] Output is reproducible under equivalent build conditions.
- [ ] Supported optimizations do not break runtime behavior.

### Acceptance Criteria

A DejaView application must be compilable into production-ready JavaScript using a reproducible Maven workflow.

---

# Phase VI — Server Rendering and Static Generation

## Sprint 16 — Server Platform Abstraction

**Objective:** Prepare the framework for server-side execution without coupling the core to a specific server implementation.

### Implementation

- [ ] Define server platform contracts.
- [ ] Define server rendering context.
- [ ] Implement server-side resource management.
- [ ] Implement request-scoped context.
- [ ] Define server-compatible component behavior.
- [ ] Define platform-specific dependency injection.
- [ ] Implement server rendering adapters.
- [ ] Define serialization boundaries.
- [ ] Separate browser-only and server-compatible APIs.

### Deliverables

- `dejaview-server`.
- Server platform contracts.
- Initial server execution environment.

### Tests

- [ ] Core executes without browser APIs.
- [ ] Server context is isolated per request.
- [ ] Server resources are disposed correctly.
- [ ] Browser-only APIs are not accidentally invoked during server execution.
- [ ] Components can be classified by platform compatibility.

### Acceptance Criteria

The framework must have a server platform foundation capable of supporting SSR.

---

## Sprint 17 — Server-Side Rendering (SSR)

**Objective:** Implement HTML rendering on the server.

### Implementation

- [ ] Implement server renderer.
- [ ] Implement HTML serialization.
- [ ] Implement text escaping.
- [ ] Implement attribute serialization.
- [ ] Implement component rendering.
- [ ] Implement nested component rendering.
- [ ] Implement request-specific rendering.
- [ ] Implement error boundaries for server rendering.
- [ ] Implement initial metadata handling.
- [ ] Implement server output validation.
- [ ] Implement initial streaming SSR where technically feasible.

### Deliverables

- `dejaview-ssr`.
- Server-side HTML renderer.
- SSR integration examples.

### Tests

- [ ] Components generate valid HTML.
- [ ] Text and attributes are escaped correctly.
- [ ] Nested components render correctly.
- [ ] Multiple requests do not leak state.
- [ ] Server rendering handles errors safely.
- [ ] Generated HTML can be loaded by a browser.
- [ ] Streaming output preserves the expected document structure.

### Acceptance Criteria

A DejaView application must be capable of generating its initial HTML on the server.

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

**Current status (2026-11-11):** Sprint 04 is complete. Sprints 01, 02, 05 and
06 have partial foundations in the repository but do not yet satisfy all of
their acceptance criteria. The authoritative implemented-versus-planned list is
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
| 07 | Component Model and Composition | Not Started |
| 08 | Inputs, Outputs, Templates and Lifecycle | Not Started |
| 09 | Dependency Injection | Not Started |
| 10 | Router | Not Started |
| 11 | HTTP Client | Not Started |
| 12 | Reactive Forms | Not Started |
| 13 | State Management | Not Started |
| 14 | CLI and Development Server | Not Started |
| 15 | Production Compiler and Optimization | Not Started |
| 16 | Server Platform Abstraction | Not Started |
| 17 | Server-Side Rendering | Not Started |
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