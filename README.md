# DejaView

<p align="center">
  <strong>A Java-first frontend framework.</strong>
  <br />
  <em>Java. Reimagined for the Web.</em>
  <br /><br />
  You've definitely seen this before.
</p>

<p align="center">
  Develop modern web applications using Java, with declarative components, fine-grained reactivity and direct integration with the JavaScript ecosystem.
</p>

---

## Overview

**DejaView** is an open-source, Java-first frontend framework designed to bring modern frontend development capabilities to the Java ecosystem.

Inspired by concepts found in Angular, React and Vue.js, DejaView aims to provide a complete frontend development experience using Java as its primary programming language.

DejaView is designed around a dedicated frontend runtime, a component-based architecture and a reactive rendering engine.

Java applications are compiled into JavaScript, allowing them to run directly in the browser and interact with native Web APIs.

**The goal is simple:** bring the developer experience of modern frontend frameworks to Java.

## Project Status

**Phase 6 in progress — Sprint 16 (server platform) and Sprint 17 (server-side rendering) implemented.**

The repository now contains a working vertical slice through the component model:

```text
Java source
  → Maven build
  → TeaVM 0.16.0
  → app.js
  → browser
  → DOM rendering
  → click
  → Signal update
  → render effect
  → reconciliation (positional; keyed for keyed children)
  → DOM text and attribute update
```

This chain is covered by automated JVM unit tests and a Chromium end-to-end
test. The CLI and the production pipeline (Sprints 14–15) are available now,
and Sprint 17 added server-side rendering: the same components produce complete
HTML on the JVM, with no JavaScript required in the browser.

| Capability | Status |
|---|---|
| Maven multi-module build, Maven Wrapper, centralized versions | Implemented |
| Java 25 → JavaScript compilation with TeaVM 0.16.0 | Implemented and validated |
| Immutable view model (elements, text, fragments, components, attributes, properties, events) | Implemented and tested |
| Platform-neutral DOM contracts, render tree and reactive renderer | Implemented and tested |
| Browser DOM adapter (TeaVM JSO) | Implemented and tested in Chromium |
| Interactive browser example | Implemented |
| `Signal` and synchronous `Effect` with dependency tracking | Implemented and tested |
| Lazy `Computed` with cache, invalidation and cycle detection | Implemented and tested |
| Batching, FIFO scheduling, cleanup and `ReactiveScope` disposal | Implemented and tested |
| Component-scoped reactive rendering and reconciliation (positional, plus keyed lists with opt-in `key`) | Implemented and tested |
| Components: one instance per mounted node, resource ownership, functional and class-based factories | Implemented and tested |
| Component inputs (`input`/`inputOr`, fail-fast `missing-input`/`invalid-input`) and outputs (`emit`, listeners in registration order) | Implemented and tested |
| Content projection (default and named slots) and lifecycle hooks (`onInit`, `onUpdate`, `onDestroy`) | Implemented and tested |
| Plain `class`/`style` attributes usable with Bootstrap, Tailwind CSS and daisyUI | Implemented; library compatibility suite planned |
| Dependency injection (`dejaview-di`): tokens, providers, `SINGLETON`/`SCOPED`/`TRANSIENT` scopes, hierarchical injectors, `inject()`/`has()`, `circular-dependency` detection, reverse-order disposal | Implemented and tested |
| Routing (`dejaview-router`): immutable route definitions, nested routes, query parameters, guards, redirects, not-found routes, route-level metadata, `RouterOutlet` with route params as inputs, browser history, keyed component swaps at a single position | Implemented and tested (Chromium E2E) |
| Equality short-circuit and topological ordering | Planned (Phase 2 refinement) |
| HTTP client (`dejaview-http`): immutable `HttpRequest`/`HttpHeaders`/`HttpResponse`, async exactly-once delivery, request/response interceptors, timeouts, credentials, cancellation, dependency-free JSON, XHR browser adapter (statuses as values; Fetch deferred) | Implemented and tested (Chromium E2E) |
| Reactive forms (`dejaview-forms`): typed `FormControl`/`FormGroup`/`FormArray`, sync and async validators, touched/dirty/disabled states, prefixed aggregated errors, `FormSubmission` snapshots, `FormBindings` DOM bindings | Implemented and tested (Chromium E2E) |
| State management (`dejaview-state`): `StateStore` shared holders, named selectors, lazy store-owned `select()` computeds, `Stores.combine` cross-store projections, `AsyncStore` with exactly-once completions and stale-while-revalidate data, `PersistedStore`/`StateCodec` opt-in persistence, `StateRegistry` composition-root registry | Implemented and tested (Chromium E2E) |
| CLI and development server (`dejaview-cli`): `dejaview new` project generator (embedded template, Maven wrapper), `dejaview build` with actionable diagnostics, `dejaview run` with live reload on a watched source tree, `dejaview doctor` project validation | Implemented and tested (Chromium E2E) |
| Production compiler pipeline (`dejaview-compiler`): centralized TeaVM configuration, `development`/`production` profiles, webapp validation and SHA-256 asset manifest on every build, reproducible output, `-Pproduction` optimized bundles | Implemented and tested (Chromium E2E on optimized bundles) |
| Server platform abstraction (`dejaview-server`): platform contracts (`PlatformKind`/`Platform`/`ServerPlatform`), request-scoped `ServerContext` (isolated state, reverse-order disposal), per-context platform compatibility registry with `BrowserOnlyComponent` marker, `ServerRenderer` producing a sealed `ServerNode` tree, strict JSON serialization boundary | Implemented and tested |
| Server-side rendering (`dejaview-ssr`): validated and escaped HTML documents and fragments from the server tree, per-request `PageMeta` metadata, `ServerErrorBoundary` containment, generic 500 failure page, JDK-hosted example that works with JavaScript disabled | Implemented and tested (alpha; no hydration or streaming) |
| DevTools | Planned |
| SSG, hydration | Planned (Sprints 18–19) |

The authoritative implemented-versus-planned breakdown is
[`docs/IMPLEMENTATION_STATUS.md`](docs/IMPLEMENTATION_STATUS.md).

## Quick Start

### Requirements

- **JDK 25** (`JAVA_HOME` must point to it).
- Network access on the first build to download Maven dependencies.
- Maven itself is not required: the Maven Wrapper is included.

```bash
java -version   # must report 25.x
```

### Build and test

```bash
# Compile all modules, run JVM unit tests, compile to JavaScript
./mvnw clean verify
```

The browser bundle is written to `examples/counter/target/webapp/`:

```text
examples/counter/target/webapp/
├── app.js
├── app.js.map
├── dejaview-assets.json
└── index.html
```

### Start your own app

The CLI scaffolds a ready-to-build project (until DejaView is published on
Central, build the CLI once from this checkout and put it on your `PATH`):

```bash
./mvnw -pl dejaview-cli -am package
alias dejaview='java -jar '"$PWD"'/dejaview-cli/target/dejaview-cli-0.1.0-SNAPSHOT.jar'
dejaview new my-app && cd my-app
dejaview doctor   # validate JDK, wrapper and layout
dejaview run      # build, serve on http://localhost:4321, reload on change
```

`dejaview run` watches `src/` and rebuilds on every change; the browser
reloads itself. On Windows, call the jar directly (`java -jar …\dejaview-cli-*.jar …`).

### Run the example

Serve the generated directory over HTTP and open it in a browser:

```bash
python3 -m http.server 4280 -d examples/counter/target/webapp
```

Then open <http://localhost:4280>. Clicking **Increment** updates the counter.

### Run the server-rendered example

`examples/ssr` renders the same component model on the JVM and serves the
result as plain HTML; no browser JavaScript is involved:

```bash
./mvnw -pl examples/ssr -am -Prun package
```

Then open <http://127.0.0.1:8080/> — with JavaScript disabled if you like.
Use `-Dssr.port=…` to change the port. The `run` profile only executes the
demo; a normal build and test run never starts a server.

### Browser end-to-end test

The Playwright test is opt-in because it downloads Chromium on first use:

```bash
./mvnw -Pe2e -pl examples/counter -am verify
```

On Windows, use `mvnw.cmd` instead of `./mvnw`.

## Website and interactive playground

The official website lives in `website/` and is itself a DejaView application
(`examples/website`), compiled to JavaScript and loaded by small static shells.
The playground executes the pre-compiled TeaVM showcase bundle from
`examples/showcase`, which the showcase E2E test validates.

```bash
# Build the showcase and website bundles
./mvnw -pl examples/showcase,examples/website -am package

# Serve the website (repository root)
python3 -m http.server 4280
```

Then open <http://localhost:4280/website/> (English) or
<http://localhost:4280/website/pt/> (Português), and
<http://localhost:4280/website/playground/> or
<http://localhost:4280/website/pt/playground/> for the playground.

The site is a demonstration: it is rendered by DejaView itself, in both
languages, straight from the compiled bundle — no manual DOM edits.

## Modules

Only modules justified by the current milestone exist. Planned modules are not
created as empty placeholders.

| Module | Responsibility | Status |
|---|---|---|
| `dejaview-core` | Immutable view model: `View`, `ElementView`, `TextView`, `FragmentView`, `ComponentView`, attributes, properties, event bindings, `Views` factories. No browser or TeaVM imports. | Implemented |
| `dejaview-reactivity` | Platform-independent `Signal<T>`, `Effect`, lazy `Computed<T>`, explicit batching, FIFO scheduling and `ReactiveScope` ownership. No browser or TeaVM imports. | Implemented |
| `dejaview-dom` | Platform-neutral DOM contracts, render tree, reactive `DomRenderer`, `TreeReconciler` (positional + keyed), TeaVM JSO browser adapter, `DejaView` / `Dom` entry points. | Implemented |
| `dejaview-di` | Platform-independent dependency injection: `InjectionToken`, `Provider`, scopes, hierarchical injectors, `inject()`/`has()`. No browser or TeaVM imports. | Implemented |
| `dejaview-router` | Client-side routing: `Route` definitions, matching, parameters, query parameters, nested routes, guards, redirects, `RouterOutlet`, browser/memory history, navigation events. TeaVM JSO only in the browser history adapter. | Implemented |
| `dejaview-http` | HTTP client: `HttpClient` with async exactly-once delivery, immutable requests and headers, interceptors, timeouts, credentials, cancellation, dependency-free JSON tree. XHR browser adapter behind `HttpTransport`; the API itself has no browser or TeaVM imports. | Implemented |
| `dejaview-forms` | Platform-independent reactive forms: `FormControl`, `FormGroup`, `FormArray`, sync/async validators, stable error keys, `FormBindings` DOM bindings. Depends on `dejaview-core` and `dejaview-reactivity`; no browser or TeaVM imports. | Implemented |
| `dejaview-state` | Platform-independent state management: `StateStore`, `Selector`, `Stores.combine`, `StateRegistry`, `AsyncStore` with `AsyncState`/`AsyncStatus`/`AsyncError`, persistence contracts (`StateStorage`, `StateCodec`, `PersistedStore`) with a `localStorage`/`sessionStorage` browser adapter. Depends on `dejaview-core` and `dejaview-reactivity`; no browser types on the API surface. | Implemented |
| `dejaview-cli` | Zero-dependency developer workflow: `dejaview new` (project generator from the embedded `basic-app` template), `dejaview build`, `dejaview run` (JDK HTTP development server with `WatchService` rebuilds and poll-based live reload) and `dejaview doctor` (JDK, wrapper, layout and configuration validation). Invokes the project's Maven wrapper; no reflection, no daemon. | Implemented |
| `dejaview-compiler` | Build-time-only production compiler pipeline (no runtime dependencies, no browser API): `WebAppValidator`/`VerifyWebApp` webapp validation, deterministic `dejaview-assets.json` SHA-256 manifest, `CompilerProfile` development/production mapping, reproducibility verification. TeaVM configuration itself lives in the parent POM (`pluginManagement`); browser modules declare only `mainClass`. | Implemented |
| `dejaview-server` | Platform-independent server execution: platform contracts (`PlatformKind`, `Platform`, `ServerPlatform`), request-scoped `ServerContext` (child injector, isolated `StateStorage`, reverse-order disposal), `PlatformCompatibilityRegistry` + `BrowserOnlyComponent` marker, `ServerRenderer` (sealed `ServerNode` tree; component compatibility enforced), server error boundaries (`ServerErrorBoundary`/`BoundaryError`), strict `SerializationBoundary`, transport-neutral `ServerRequest`/`ServerResponse`. Depends on `dejaview-core`, `dejaview-di`, `dejaview-state`, `dejaview-http`; no browser or TeaVM imports. | Implemented |
| `dejaview-ssr` | Server-side HTML rendering: `SsrRenderer` (page and fragment rendering through `ServerRenderer`), `SsrValidator` (tag/attribute names, void elements, duplicates, depth 512), `HtmlSerializer`/`HtmlPage` (escaped text and attributes, complete document), per-request `PageMeta`, `SsrResult` with status/HTML/warnings/attached error, generic 500 failure page. Depends on `dejaview-server`; no browser or TeaVM imports. | Implemented (alpha) |
| `examples/counter` | Starter example compiled to JavaScript and tested in Chromium. | Implemented |
| `examples/showcase` | Pre-compiled showcase bundle: the code samples the website and playground embed. | Implemented |
| `examples/website` | Official website, documentation and interactive playground — rendered by DejaView itself, in English and Portuguese. | Implemented |
| `examples/ssr` | Server-rendered application served by the JDK HTTP server; pages, search, error boundary and 404 work with JavaScript disabled. | Implemented |

Dependency direction:

```text
examples/counter → dejaview-dom → dejaview-reactivity → dejaview-core
examples/showcase → dejaview-dom, dejaview-router, dejaview-http, dejaview-forms, dejaview-state → dejaview-core
examples/website → dejaview-dom, dejaview-reactivity → dejaview-core
dejaview-di → dejaview-core
dejaview-router → dejaview-core, dejaview-reactivity
dejaview-http → dejaview-core
dejaview-forms → dejaview-core, dejaview-reactivity
dejaview-state → dejaview-core, dejaview-reactivity
dejaview-cli → (no runtime dependencies)
dejaview-compiler → (no runtime dependencies; build-time only)
dejaview-server → dejaview-core, dejaview-di, dejaview-state, dejaview-http
dejaview-ssr → dejaview-server
examples/ssr → dejaview-ssr
```

`dejaview-reactivity` and `dejaview-di` are independent of the view model and
the DOM. The renderer uses reactivity to track signals read by component views
and to reconcile the mounted tree; application code no longer performs direct
DOM updates.

The target architecture keeps the core independent of browser APIs and places
platform-specific code in dedicated modules. `dejaview-server` and
`dejaview-ssr` are the server-side modules; the remaining long-term module map
(`dejaview-ssg`, `dejaview-hydration`, ...) is still a design proposal; see
[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

### Repository layout

```text
dejaview/
├── dejaview-core/          # Framework core (pure Java)
├── dejaview-reactivity/    # Signals and effects (pure Java)
├── dejaview-dom/           # DOM contracts, renderer, browser adapter
├── dejaview-di/            # Dependency injection (pure Java)
├── dejaview-router/        # Client-side router (pure Java + TeaVM history adapter)
├── dejaview-http/          # HTTP client (pure Java API + TeaVM XHR adapter)
├── dejaview-forms/         # Reactive forms (pure Java)
├── dejaview-state/         # State management (pure Java)
├── dejaview-cli/           # CLI and development server (zero dependencies)
├── dejaview-compiler/      # Build-time production compiler pipeline (zero dependencies)
├── dejaview-server/        # Server platform abstraction (pure Java)
├── dejaview-ssr/           # Server-side HTML rendering (pure Java)
├── examples/
│   ├── counter/            # Java → JavaScript browser example
│   ├── showcase/           # Pre-compiled showcase bundle embedded by the site
│   ├── website/            # Website, documentation and playground, rendered by DejaView
│   └── ssr/                # Server-rendered example (JVM only, no TeaVM build)
├── website/                # Static shells, styles and assets served from this directory
├── docs/
│   ├── ARCHITECTURE.md     # Long-term architecture specification
│   ├── COMPILER.md         # TeaVM feasibility report and constraints
│   ├── IMPLEMENTATION_STATUS.md
│   └── ADR/                # Architectural Decision Records
├── AGENTS.md               # Instructions and Definition of Done for coding agents
├── .mvn/
├── mvnw
├── mvnw.cmd
└── pom.xml
```

## Technology Stack

| Technology | Purpose |
|---|---|
| Java 25 | Primary development language |
| Apache Maven | Build system and dependency management |
| TeaVM 0.16.0 | Initial Java-to-JavaScript compiler |
| JavaScript | Browser execution target |
| HTML / CSS | Interface structure and styling |
| DOM / Web APIs | Browser integration |

JavaScript is the framework's **only planned browser compilation target**.
WebAssembly is explicitly out of scope.

## How It Works Today

The implemented flow is deliberately small:

1. A component builds an immutable view tree with `Views` factories. State lives
   in signals owned by the component instance.
2. `DejaView.mount(Views.componentFactory(MyComponent::new), "app")` resolves the
   container and asks `DomRenderer` to mount the tree.
3. `DomRenderer` works only against `DomDocument` / `DomElement` / `DomNode`
   contracts, so it is tested on the JVM with a fake DOM.
4. In the browser, `BrowserDomElement` implements those contracts using TeaVM
   JSO (`HTMLDocument`, `HTMLElement`, `Node`, `EventListener`).
5. A signal read while the component renders is tracked by the mounted tree's
   single `Effect`. The click handler only writes to the signal; the renderer
   re-renders the component and reconciles the existing DOM nodes — in place
   when keys and tag names match.
6. Inputs flow down as raw values re-bound on every render pass
   (`component.input("username", value)`), outputs flow up as registered
   `Consumer`s (`component.on("select", listener)`), and projected content is
   spliced into the component's tree via slots (`slot("header")` / `slot()`).
7. On the server, `ServerRenderer` mounts the same view tree without a DOM and
   `dejaview-ssr` serializes it to validated, escaped HTML; components write
   per-request `PageMeta` while rendering and a `ServerErrorBoundary` can
   contain a failing subtree. `examples/ssr` serves this over the JDK HTTP
   server and works with JavaScript disabled.

The renderer owns the reactive binding: application code no longer looks up DOM
nodes. The flow is recorded in
[ADR-0013](docs/ADR/0013-reactive-rendering-and-reconciliation.md) and
[ADR-0015](docs/ADR/0015-component-interaction-model.md).

## Conceptual API (Not Implemented)

The class-based component and lifecycle parts of this sketch exist today; the
annotation API does not. The programmatic API is the supported surface:

```java
final class UserCard extends AbstractComponent {

    @Override
    public View render() {
        String username = input("username", String.class);
        return Views.div(
            Views.h2("@" + username),
            Views.button("Select").onClick(() -> emit("select", username))
        );
    }
}
```

```java
// parent
Views.componentFactory(UserCard.class, UserCard::new)
     .input("username", "Ada")
     .on("select", selected -> System.out.println("selected: " + selected));
```

What does **not** exist: `@Component`, `@Input`, `@Output`, `@OnInit` and their
processing. Annotations remain a proposal layered on top of the programmatic
API — see
[`docs/IMPLEMENTATION_STATUS.md`](docs/IMPLEMENTATION_STATUS.md).

## Design Principles

1. Java is the primary language for application development.
2. JavaScript is the only browser compilation target.
3. The core must remain independent of browser-specific APIs.
4. Reactivity should be granular rather than relying on unnecessary full-tree updates.
5. Components should be composable and independently manageable.
6. Rendering strategies should share a consistent component model.
7. Tooling and developer experience are first-class concerns.
8. Public APIs should remain consistent across framework modules.
9. Maven is the standard build and dependency management system.

## Roadmap

| Phase | Description | Status |
|---|---|---|
| 1 | Foundation and JavaScript runtime | Implemented |
| 2 | Reactivity Engine | Completed — `Signal`, `Effect`, `Computed`, batching, scheduling, scopes and component-scoped renderer bindings implemented |
| 3 | Component System | Implemented — Sprints 07–08 (component model, inputs, outputs, slots, lifecycle, keyed lists) |
| 4 | Application Framework | Completed — Sprints 09–13 implemented (dependency injection, router, HTTP client, reactive forms, state management) |
| 5 | Compiler and Tooling | Completed — Sprints 14 (CLI and development server) and 15 (production compiler) delivered |
| 6 | SSR and SSG | In progress — Sprint 16 (Server Platform Abstraction) and Sprint 17 (Server-Side Rendering) delivered; Sprints 18–19 planned |
| 7 | Advanced Features | Planned |
| 8 | Ecosystem | Planned |

### Next milestone: Sprint 18 — Static Site Generation (SSG)

Sprint 17 delivered server-side rendering (`dejaview-ssr`, pure Java 25, zero
browser/TeaVM imports; see
[ADR-0024](docs/ADR/0024-server-side-rendering.md)): `SsrRenderer` composes
`ServerRenderer` with `SsrValidator` and the HTML writers, so a `View` renders
to a complete, escaped document on the JVM. Tag and attribute names are
validated (`ssr-validation-failed` for illegal names, void elements with
children, duplicate attributes and depth beyond 512), text and attribute values
are always escaped, and `html`/`script` content that cannot be represented
safely is reported as a warning instead of being emitted raw. `PageMeta` carries
request-scoped lang, title, description, canonical and meta/Open Graph tags into
the document head. `dejaview-server` gained `ServerErrorBoundary`: a failing
subtree is destroyed and closed LIFO, recorded as a `BoundaryError`, and its
`fallback(error)` is rendered in place, keeping the rest of the page alive.
Unhandled failures return status 500 with a fixed generic page; the exception
stays in the host's logs. `examples/ssr` is a working application served by the
JDK `HttpServer`, with a GET search form, a contained widget failure and a 404
page, and its Chromium test runs with JavaScript disabled. 43 JVM tests cover
`dejaview-ssr`, 10 cover the example, and the server module grew to 80. Not
delivered: SSG (Sprint 18), hydration and state serialization (Sprint 19),
progressive streaming (Phase 7), a deployable server adapter (Sprint 21) and
the full DOM property set — SSR is alpha (ADR-0024).

Sprint 16 delivered the server platform abstraction
(`dejaview-server`, pure Java 25, zero browser/TeaVM imports; see
[ADR-0023](docs/ADR/0023-server-platform-abstraction.md)): platform contracts
(`PlatformKind`/`Platform`/`ServerPlatform`), request-scoped `ServerContext`
(a child injector `server.request` binding `REQUEST`/`CONTEXT` inside the
child only, an owned `ResourceRegistry` with reverse-order disposal, and
isolated state — a fresh `MemoryStateStorage` unless the parent injector
resolves `StateStorage`; `ServerBindings.apply` binds it with `Scope.SCOPED`),
a per-context `PlatformCompatibilityRegistry` with the `BrowserOnlyComponent`
marker (exact-class override > marker assignability > `UNIVERSAL`), and a
stateless `ServerRenderer` that renders the platform-neutral `View` tree into
a sealed `ServerNode` tree (`ServerElement`/`ServerText`/`ServerFragment`).
Browser-only components fail fast with `browser-only-component` — before
construction when the view carries a class token, before
`bind`/`onInit`/`render` otherwise; event handlers are dropped and reported
de-duplicated in `ServerTree.droppedEvents()`; mounted components receive
`onDestroy()` (when initialized) and `close()` in reverse creation order on
success and failure alike. Everything crossing the boundary leaves through
`SerializationBoundary` (strict `toJson`/`write`; `not-serializable` otherwise;
depth cap 512). `ServerRequest`/`ServerResponse` are transport-neutral values,
not yet wired to an HTTP server. 80 JVM tests cover the module today. HTML
serialization arrived in Sprint 17 (ADR-0024); SSG, hydration and a deployable
server adapter remain open.

Sprint 15 delivered the production compilation pipeline (`dejaview-compiler`,
build-time only, zero runtime dependencies; see
[ADR-0022](docs/ADR/0022-production-compiler-pipeline.md)): TeaVM configuration
is centralized in the parent POM (`pluginManagement`), browser modules declare
only `mainClass`, and two Maven profiles share the pipeline — `development`
(default: `SIMPLE`, readable unminified JavaScript) and `production`
(`-Pproduction`: `FULL` dead-code elimination and minified output). Every build
validates the assembled webapp and writes `dejaview-assets.json` (paths, sizes,
SHA-256) next to the bundle; `ReproducibleBuildIT` proves byte-identical output
across consecutive builds. During the sprint a long-standing bug was found: the
TeaVM plugin parameter is `minifying` (default `true`), so the previously used
`<minified>` element was silently ignored and every development build shipped
obfuscated JavaScript — fixed in the parent POM and the generated template.
Generated projects (`dejaview new`) validate on every `package`. See
[ADR-0022](docs/ADR/0022-production-compiler-pipeline.md) and
[docs/COMPILER.md](docs/COMPILER.md).

Sprint 14 delivered the official developer workflow (`dejaview-cli`, zero
runtime dependencies): `dejaview new <name>` generates a self-contained
project from the embedded `basic-app` template (generated `./mvnw` wrapper,
a standalone pom, a working counter component and the host page — empty
directory required; `.gitignore` restored; `mvnw` marked executable);
`dejaview build` runs the wrapper and reports the actionable `[ERROR]` lines
(Maven boilerplate filtered) when it fails; `dejaview run` builds once,
then serves `target/webapp` on `http://localhost:4321` with a recursive,
300 ms-debounced `WatchService` watching `src/`, rebuilding on change and
reloading the browser through an injected poll-based client and a
`/__dejaview__/version` endpoint — a failed rebuild keeps the last good
build serving; `dejaview doctor` validates the JDK (≥ 25), the compiler,
the wrapper, the project layout, `dejaview.properties` (`port`/`host`/
`webappDir`, flags win) and local dependency availability. The whole loop
is verified in Chromium by `CliWorkflowIT` (generate → wrapper build →
serve → render → source edit → rebuild → live reload), and the bilingual
guide documents the workflow in section 16. See
[ADR-0021](docs/ADR/0021-cli-and-dev-server.md).

Sprint 13 delivered a platform-independent state management module (`dejaview-state`,
depending on `dejaview-core` and `dejaview-reactivity`): `StateStore` shared
holders with tracked reads and synchronous writes, named `Selector` projectors,
lazy store-owned `select()` computeds, `Stores.combine` for cross-store derived
values, `StateRegistry` (an ordinary composition-root registry, no global
singletons, reverse-order `disposeAll()`), `AsyncStore` (exactly-once
completions via a settled-attempt guard, generation-counter staleness
rejection, stale-while-revalidate data) and `AsyncState`/`AsyncStatus`/
`AsyncError` (stable machine-readable codes; applications map codes to
localized copy, like form error keys). Persistence is opt-in: `PersistedStore`
restores from a `StateStorage` before the first observer exists, then writes
every change through; decode/storage failures surface as codes
(`decode-error`, `storage-error`, `storage-write-error`), never as crashes.
The showcase `StateDemo` (`?example=state`) demonstrates a shared cart store,
selectors, a cross-store discount, an async checkout and a `localStorage`-
persisted theme, verified in Chromium by
`ShowcaseBrowserIT#stateExampleManagesSharedDerivedAndAsyncState` — including
restore across reload. The website feature card, tutorial section (heading 15 in both
languages) and playground example are wired; `WebsiteBrowserIT`
asserted 11 feature cards and 10 playground examples (13 cards since Sprint 15).
See [ADR-0020](docs/ADR/0020-state-management-model.md).

Earlier in this phase, Sprint 12 delivered a platform-independent reactive forms module (`dejaview-forms`,
depending on `dejaview-core` and `dejaview-reactivity`): `FormControl`,
`FormGroup` and `FormArray` with value, `touched`/`dirty`/`disabled` states,
aggregated `status()` (`VALID`/`INVALID`/`PENDING`/`DISABLED`) and prefixed
`errors()` (`email.required`, `tags.0.required`), synchronous validators
(`required`, `minLength`, `maxLength`, `email`, `pattern`, `min`, `max`,
`custom`), asynchronous validators with generation-counter staleness rejection
(synchronous validators must pass before async runs), `reset()`/`dispose()`
lifecycle semantics and `FormSubmission` snapshots with an `ifValid` guard.
`FormBindings` connects DOM controls to controls: `input`/`textarea` on
`input`, `checkbox`/`select` on `change`, blur marks touched, and
`aria-invalid`/`disabled` reflect the control status. The showcase
`FormsDemo` (`?example=forms`) demonstrates sync + async validation, a dynamic
tag `FormArray` and a submission snapshot, verified in Chromium by
`ShowcaseBrowserIT#formsExampleValidatesAndSubmits`. The website feature card,
tutorial section (heading 14 in both languages) and playground example are
wired. See [ADR-0019](docs/ADR/0019-reactive-forms.md).

Earlier in this phase, Sprint 11 delivered a platform-independent HTTP client (`dejaview-http`,
depending on `dejaview-core`): immutable `HttpRequest` (`get`/`post`/`put`/
`patch`/`delete`, fluent withers, percent-encoded query parameters with `+`
for space), immutable case-insensitive `HttpHeaders` with multi-valued
entries, `HttpResponse` with `ok()` (2xx) and a dependency-free `JsonValue`
tree/parser/writer (finite numbers, 512-level depth cap, offsets on errors),
and `HttpClient` with async exactly-once consumer delivery, registration-ordered
request and response interceptors (response interceptors observe completed
round-trips only, HTTP error statuses included), per-request vs client-level
timeout and credentials resolution, and cancellation via `HttpCall`.
HTTP error statuses are `Success` values — `Failure` is reserved for
transport-level failures (`network-error`, `timeout`, `cancelled`,
`interceptor-error`, `missing-transport`, `json-error`). The browser adapter
uses `XMLHttpRequest` because `teavm-jso-apis` 0.16.0 ships no Fetch binding;
the deviation, the XHR mapping (e.g. same-origin cookies cannot be omitted;
only `INCLUDE` sets `withCredentials`; status 0 means `network-error`) and
the consequences are recorded in [ADR-0018](docs/ADR/0018-http-client-model.md).
The playground's HTTP example fetches the showcase's `ping.json` and renders
the body; the full story is in
[ADR-0018](docs/ADR/0018-http-client-model.md).

Earlier in this phase, Sprint 10 delivered client-side routing as a
platform-independent module (`dejaview-router`, depending on `dejaview-core`
and `dejaview-reactivity`): immutable `Route` definitions with literal,
`:param` and `**` segments, nested and index routes, fail-fast
`invalid-route` validation, resolve-before-push navigation
(`navigate`/`replace`/`back`/`forward`) that leaves state and history
unchanged on denial or no-match, root-first guards with prefix-accumulated
contexts (`Allow`/`Deny`/`Redirect`; ten-hop `redirect-loop` budget), an
optional `notFound(...)` chain, `NavigationEvent`s with a reversible
listener, `RouterOutlet` rendering at a chain level keyed by the matched
route's id (path parameters pushed as inputs), and browser history behind
the `RouterHistory` seam (`MemoryRouterHistory` for tests, `BrowserHistory`
for TeaVM). Single-position keyed components now replace the mounted
instance when the key changes. Lazy route loading is limited to
activation-time factory deferral; bundle-level code splitting is not supported
by the single-bundle TeaVM output and remains open. See [ADR-0017](docs/ADR/0017-routing-model.md).

The remaining Phase 2 refinements are still open:

1. Equality short-circuiting so dependents do not rerun when a computed value is
   unchanged.
2. Optional topological ordering for glitch-free computed propagation.

Details are in [`docs/IMPLEMENTATION_STATUS.md`](docs/IMPLEMENTATION_STATUS.md),
[ADR-0008](docs/ADR/0008-synchronous-signals-and-effects.md),
[ADR-0009](docs/ADR/0009-lazy-computed-values.md),
[ADR-0010](docs/ADR/0010-batching-scheduling-and-scopes.md),
[ADR-0013](docs/ADR/0013-reactive-rendering-and-reconciliation.md) and
[ADR-0015](docs/ADR/0015-component-interaction-model.md).

## Build Commands

```bash
# Full build: compile, unit tests, TeaVM output, artifact checks
./mvnw clean verify

# Run JVM unit tests only
./mvnw test

# Build one module and its dependencies
./mvnw -pl dejaview-core -am package

# Skip tests
./mvnw clean package -DskipTests

# Opt-in browser end-to-end tests (unit + examples)
./mvnw -Pe2e -pl dejaview-compiler,dejaview-cli,examples/counter,examples/showcase,examples/website -am verify
```

The root `pom.xml` declares the modules, centralizes dependency and plugin
versions, configures the Java compiler and enforces build prerequisites.
Individual modules declare only their own dependencies and build requirements.

## Documentation

| Document | Description |
|---|---|
| [`AGENTS.md`](AGENTS.md) | Working rules and the feature → website → documentation Definition of Done |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | Long-term architecture specification |
| [`docs/COMPILER.md`](docs/COMPILER.md) | TeaVM feasibility, validated matrix and constraints |
| [`docs/IMPLEMENTATION_STATUS.md`](docs/IMPLEMENTATION_STATUS.md) | Implemented vs planned, limitations, next milestone |
| [`docs/ADR/`](docs/ADR/README.md) | Architectural Decision Records |

Significant decisions are recorded as ADRs rather than changed silently.

## Known Limitations

- Reactivity is synchronous and single-threaded: batching is explicit via
  `Reactive.batch`, and there is no microtask or time-based scheduler.
- Computed invalidation is push-based while evaluation is lazy, so dependents
  may rerun even when the recomputed value is unchanged.
- Effects run in invalidation order; there is no topological dependency ordering
  or glitch-free propagation yet.
- Ownership is explicit through `ReactiveScope`; components do not own scopes
  automatically.
- Reactive updates are component-scoped: a signal read while rendering reruns
  the component and reconciles its subtree. Fine-grained binding nodes do not
  exist yet.
- Reconciliation is positional, or keyed when views carry `key` (lists switch
  fully to keyed matching; a key change at a single position swaps the
  mounted component). Views without keys reorder by position; there is no
  public unmount API or hydration.
- Tokenless unkeyed views at the same position reuse the mounted instance
  even if the new view renders a different component class — dynamic swaps
  require a distinct key or a class token (`componentFactory(Class, Supplier)`).
- Annotation-based API (`@Component`, `@Input`, `@Output`) and Context API do
  not exist; the programmatic surface is the supported API.
- HTTP is async-only and XHR-based: HTTP error statuses are `Success` (branch
  on `ok()`), XHR cannot omit same-origin cookies (`OMIT` and `SAME_ORIGIN`
  both map to `withCredentials=false`), response bodies arrive as text with
  no streaming, and `cancelled` arrives asynchronously after `cancel()`.
  See [ADR-0018](docs/ADR/0018-http-client-model.md).
- Styling is plain HTML plus `class`/`style` attributes; there is no CSS
  pipeline or CSS-in-JS runtime. Bootstrap, Tailwind CSS and daisyUI are not yet
  covered by automated compatibility tests.
- The website playground runs pre-compiled TeaVM bundles only; there is no in-browser compiler yet, so editing the Java source is a read-only preview until the development server is built.
- Browser E2E coverage is Chromium only.
- Only the JDK/TeaVM APIs exercised by the current code are validated.
- The production bundle is a single file: TeaVM 0.16.0 has no code splitting,
  so every application ships one `app.js` (smaller under `-Pproduction` via
  FULL dead-code elimination, minification included).
- Development builds are intentionally unminified and readable; production
  builds are minified but still carry a source map (`app.js.map`).

See [`docs/IMPLEMENTATION_STATUS.md`](docs/IMPLEMENTATION_STATUS.md) for the
complete list.

## Contributing

DejaView is in its early development stage. Start with [`AGENTS.md`](AGENTS.md),
which describes the module boundaries, the build and browser-validation
commands, and the Definition of Done.

The project is bilingual and documentation-driven: a feature is only complete
when the same change set updates the code, the tests, the English and Portuguese
website (feature card, guide section and playground example when interactive)
and the internal documentation. Java samples and their comments stay in English;
the prose around them is translated.

Before contributing to architectural components, please review the documentation
and the corresponding Architectural Decision Records.

## License

License: **To be determined.**

---

<p align="center">
  <strong>DejaView</strong>
  <br />
  <em>Java. Reimagined for the Web.</em>
  <br /><br />
  You've definitely seen this before.
  <br />
  But never quite like this.
</p>
