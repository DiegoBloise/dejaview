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

**Foundation milestone implemented; Phase 2 in progress.**

The repository now contains a working vertical slice:

```text
Java source
  → Maven build
  → TeaVM 0.16.0
  → app.js
  → browser
  → DOM rendering
  → click
  → Signal update
  → Effect rerun
  → DOM text update
```

This chain is covered by automated JVM unit tests and a Chromium end-to-end test.
It is a **foundation**, not a usable application framework yet: there is a
minimal reactivity core (signals and effects), but no component system,
routing, SSR or tooling.

| Capability | Status |
|---|---|
| Maven multi-module build, Maven Wrapper, centralized versions | Implemented |
| Java 25 → JavaScript compilation with TeaVM 0.16.0 | Implemented and validated |
| Immutable view model (elements, text, fragments, attributes, events) | Implemented and tested |
| Platform-neutral DOM contracts and first-mount renderer | Implemented and tested |
| Browser DOM adapter (TeaVM JSO) | Implemented and tested in Chromium |
| Interactive browser example | Implemented |
| `Signal` and synchronous `Effect` with dependency tracking | Implemented and tested |
| Lazy `Computed` with cache, invalidation and cycle detection | Implemented and tested |
| Batching, scheduling, effect ownership, renderer bindings | Planned (rest of Phase 2) |
| Components, lifecycle, inputs/outputs/slots | Planned (Phase 3) |
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

## Modules

Only modules justified by the current milestone exist. Planned modules are not
created as empty placeholders.

| Module | Responsibility | Status |
|---|---|---|
| `dejaview-core` | Immutable view model: `View`, `ElementView`, `TextView`, `FragmentView`, attributes, event bindings, `Views` factories. No browser or TeaVM imports. | Implemented |
| `dejaview-reactivity` | Platform-independent `Signal<T>`, synchronous `Effect` and lazy `Computed<T>` with automatic dependency tracking. No browser or TeaVM imports. | Implemented |
| `dejaview-dom` | Platform-neutral DOM contracts, first-mount `DomRenderer`, TeaVM JSO browser adapter, `DejaView` / `Dom` entry points. | Implemented |
| `examples/counter` | End-to-end example compiled to JavaScript and tested in Chromium. | Implemented |

Dependency direction:

```text
examples/counter → dejaview-dom → dejaview-core
                 → dejaview-reactivity
```

`dejaview-reactivity` is independent of both the view model and the DOM. The
renderer will consume it in a later slice to replace the example's direct DOM
binding.

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
│   └── counter/            # Java → JavaScript browser example
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

1. `CounterApp` builds an immutable view tree with `Views` factories.
2. `DejaView.mount(view, "app")` resolves the container and asks `DomRenderer`
   to append the tree.
3. `DomRenderer` works only against `DomDocument` / `DomElement` / `DomNode`
   contracts, so it is tested on the JVM with a fake DOM.
4. In the browser, `BrowserDomElement` implements those contracts using TeaVM
   JSO (`HTMLDocument`, `HTMLElement`, `Node`, `EventListener`).
5. The count is a `Signal<Integer>`. An `Effect` reads it and updates the text
   node; the click handler only calls `count.update(...)`.

The effect currently resolves the text node with `Dom.byId("count")`. That is a
temporary bridge until the renderer owns reactive text bindings. It is recorded
in [ADR-0008](docs/ADR/0008-synchronous-signals-and-effects.md); the earlier
manual update shortcut is in [ADR-0007](docs/ADR/0007-first-milestone-scope.md).

## Conceptual API (Not Implemented)

The following snippet illustrates the intended long-term developer experience.
**It is a design sketch, not a working API.** `Signal` and `Effect` exist as
plain Java classes, but `@Component`, the `signal(...)` factory and reactive
`View` rendering do not.

```java
@Component
public class CounterComponent {

    private final Signal<Integer> count = signal(0);

    public View render() {
        return div(
            h1("DejaView Counter"),

            p(() -> "Count: " + count.get()),

            button("Increment")
                .onClick(() -> count.update(value -> value + 1))
        );
    }
}
```

The intended developer experience focuses on:

- Java-based component definitions.
- Strong typing.
- Declarative UI composition.
- Reactive state management.
- Automatic interface updates.

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
| 1 | Foundation and JavaScript runtime | Foundation implemented (first-mount rendering) |
| 2 | Reactivity Engine | In progress — `Signal`, `Effect` and `Computed` implemented; batching, scheduling and renderer bindings pending |
| 3 | Component System | Planned |
| 4 | Application Framework | Planned |
| 5 | Compiler and Tooling | Planned |
| 6 | SSR and SSG | Planned |
| 7 | Advanced Features | Planned |
| 8 | Ecosystem | Planned |

### Next milestone: finish Phase 2 — Reactivity

The first reactivity slice is implemented: `Signal<T>` (`get`, `set`, `update`),
a synchronous `Effect` with automatic dependency tracking and disposal, and a
lazy `Computed<T>` with caching, invalidation and cycle detection. The counter
example updates through an effect instead of mutating the DOM in the click
handler.

The remaining Phase 2 work is:

1. A platform-independent scheduler/batching boundary.
2. Ownership/scopes so effects and computeds can be disposed with their component.
3. Renderer-owned reactive bindings, removing `Dom.byId` from application code.
4. Tests for dependency graphs, batching and cleanup.
5. A browser E2E test where a signal update reaches the DOM through a renderer
   binding rather than a manual lookup.

Details are in [`docs/IMPLEMENTATION_STATUS.md`](docs/IMPLEMENTATION_STATUS.md)
and [ADR-0008](docs/ADR/0008-synchronous-signals-and-effects.md).

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

# Opt-in browser end-to-end test
./mvnw -Pe2e -pl examples/counter -am verify
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

- Minimal reactivity: `Signal`, `Effect` and `Computed` exist, but there is no
  batching, scheduler, ownership tree or computed disposal lifecycle.
- Computed invalidation is push-based while evaluation is lazy, so dependents
  may rerun even when the recomputed value is unchanged.
- The example's effect still resolves the DOM node with `Dom.byId("count")`;
  renderer-owned reactive bindings do not exist yet.
- First mount only: no diffing, patching, unmounting or hydration.
- No component abstraction, routing, DI, HTTP client, forms or state management.
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
