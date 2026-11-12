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

**Phase 3 in progress — Sprint 08 (inputs, outputs, lifecycle, keyed lists) implemented.**

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
test. Routing, DI, SSR or tooling do not exist yet.

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
| Equality short-circuit and topological ordering | Planned (Phase 2 refinement) |
| Routing, DI, HTTP, forms, state management | Planned (Phase 4) |
| CLI, DevTools, compiler abstraction | Planned |
| SSR, SSG, hydration | Planned |

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
└── index.html
```

### Run the example

Serve the generated directory over HTTP and open it in a browser:

```bash
python3 -m http.server 8080 -d examples/counter/target/webapp
```

Then open <http://localhost:8080>. Clicking **Increment** updates the counter.

### Browser end-to-end test

The Playwright test is opt-in because it downloads Chromium on first use:

```bash
./mvnw -Pe2e -pl examples/counter -am verify
```

On Windows, use `mvnw.cmd` instead of `./mvnw`.

## Website and interactive playground

The official website and playground live in `website/` and are served as static
files (no build step required). The playground examples execute the same
pre-compiled TeaVM bundle that the counter E2E test validates.

```bash
# Build the showcase example bundle
./mvnw -pl examples/showcase -am package

# Serve the website (repository root)
python3 -m http.server 8099
```

Then open <http://localhost:8099/website/> and <http://localhost:8099/website/playground/>.

## Modules

Only modules justified by the current milestone exist. Planned modules are not
created as empty placeholders.

| Module | Responsibility | Status |
|---|---|---|
| `dejaview-core` | Immutable view model: `View`, `ElementView`, `TextView`, `FragmentView`, `ComponentView`, attributes, properties, event bindings, `Views` factories. No browser or TeaVM imports. | Implemented |
| `dejaview-reactivity` | Platform-independent `Signal<T>`, `Effect`, lazy `Computed<T>`, explicit batching, FIFO scheduling and `ReactiveScope` ownership. No browser or TeaVM imports. | Implemented |
| `dejaview-dom` | Platform-neutral DOM contracts, render tree, reactive `DomRenderer`, `TreeReconciler` (positional + keyed), TeaVM JSO browser adapter, `DejaView` / `Dom` entry points. | Implemented |
| `examples/counter` | End-to-end example compiled to JavaScript and tested in Chromium. | Implemented |
| `examples/showcase` | Official website, documentation and interactive playground (pre-compiled examples) | Implemented |

Dependency direction:

```text
examples/counter → dejaview-dom → dejaview-reactivity → dejaview-core
examples/showcase → dejaview-dom → dejaview-reactivity → dejaview-core
```

`dejaview-reactivity` is independent of the view model and the DOM. The renderer
uses it to track signals read by component views and to reconcile the mounted
tree; application code no longer performs direct DOM updates.

The target architecture keeps the core independent of browser APIs and places
platform-specific code in dedicated modules. The remaining long-term module map
(`dejaview-renderer`, `dejaview-router`, `dejaview-ssr`, ...) is still a design
proposal; see [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

### Repository layout

```text
dejaview/
├── dejaview-core/          # Framework core (pure Java)
├── dejaview-reactivity/    # Signals and effects (pure Java)
├── dejaview-dom/           # DOM contracts, renderer, browser adapter
├── examples/
│   ├── counter/            # Java → JavaScript browser example
│   └── showcase/           # Official website, documentation, playground
├── website/                # Static website served from this directory
├── docs/
│   ├── ARCHITECTURE.md     # Long-term architecture specification
│   ├── COMPILER.md         # TeaVM feasibility report and constraints
│   ├── IMPLEMENTATION_STATUS.md
│   └── ADR/                # Architectural Decision Records
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
| 3 | Component System | In progress — Sprint 08 implemented (inputs, outputs, slots, lifecycle, keyed lists, dynamic component swaps); annotations and Context API remain |
| 4 | Application Framework | Planned |
| 5 | Compiler and Tooling | Planned |
| 6 | SSR and SSG | Planned |
| 7 | Advanced Features | Planned |
| 8 | Ecosystem | Planned |

### Next milestone: Sprint 09 — Dependency Injection

Sprint 08 delivered the component interaction model: typed inputs
(`input(String, Class)` / `inputOr`) with fail-fast validation, outputs emitted
to view-registered listeners in registration order, default and named slots
projected directly into the rendered tree, explicit lifecycle hooks
(`onInit` / `onUpdate` / `onDestroy`), keyed list reconciliation and dynamic
component replacement via explicit class tokens
(`Views.componentFactory(Class, Supplier)`). The `usercard` and `list` showcase
examples exercise that surface and are covered by Playwright E2E in Chromium.

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
./mvnw -Pe2e -pl examples/counter,examples/showcase -am verify
```

The root `pom.xml` declares the modules, centralizes dependency and plugin
versions, configures the Java compiler and enforces build prerequisites.
Individual modules declare only their own dependencies and build requirements.

## Documentation

| Document | Description |
|---|---|
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
- Reconciliation is positional, or keyed when children carry `key`. Lists
  without keys reorder by position; there is no public unmount API or hydration.
- Unkeyed tokenless views at the same position reuse the mounted instance even
  if the new view renders a different component class — dynamic swaps require
  distinct keys or a class token (`componentFactory(Class, Supplier)`).
- Annotation-based API (`@Component`, `@Input`, `@Output`) and Context API do
  not exist; the programmatic surface is the supported API.
- Styling is plain HTML plus `class`/`style` attributes; there is no CSS
  pipeline or CSS-in-JS runtime. Bootstrap, Tailwind CSS and daisyUI are not yet
  covered by automated compatibility tests.
- The website playground runs pre-compiled TeaVM bundles only; there is no in-browser compiler yet, so editing the Java source is a read-only preview until the development server is built.
- Browser E2E coverage is Chromium only.
- Only the JDK/TeaVM APIs exercised by the current code are validated.
- The example emits readable, non-minified JavaScript; production optimization
  is not benchmarked yet.

See [`docs/IMPLEMENTATION_STATUS.md`](docs/IMPLEMENTATION_STATUS.md) for the
complete list.

## Contributing

DejaView is in its early development stage.

Contribution guidelines, development setup, coding standards and issue templates
will be published as the project matures.

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
