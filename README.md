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

## Core Philosophy

- **Java-first:** Build frontend applications using Java.
- **Reactive by design:** Fine-grained reactivity powered by Signals.
- **Component-driven:** Build interfaces through reusable and composable components.
- **Performance-oriented:** Efficient rendering, optimized builds and minimal unnecessary DOM updates.
- **Developer experience:** Modern tooling, CLI, debugging and development workflows.
- **Modular architecture:** Use only the capabilities your application needs.
- **Web standards:** Work directly with JavaScript, DOM and native browser APIs.

## Technology Stack

| Technology | Purpose |
|---|---|
| Java | Primary development language |
| Apache Maven | Build system and dependency management |
| TeaVM | Initial Java-to-JavaScript compiler |
| JavaScript | Browser execution target |
| HTML / CSS | Interface structure and styling |
| DOM / Web APIs | Browser integration |

JavaScript is the framework's **only planned browser compilation target**.

DejaView does not aim to introduce an alternative browser execution environment. Instead, it prioritizes direct integration with the existing web platform.

## Architecture

DejaView is designed as a modular framework with clearly separated responsibilities.

```text
                  DEJAVIEW
                      |
          +-----------+-----------+
          |                       |
       CORE                    TOOLING
          |                       |
    +-----+------+          +-----+------+
    |            |          |            |
 REACTIVITY   COMPONENTS   CLI        DEVTOOLS
    |            |          |            |
    +------+-----+          +------------+
           |
       RENDERER
           |
     PLATFORM LAYER
           |
     +-----+------+
     |            |
    DOM         SERVER
     |            |
    CSR          SSR
                  |
                SSG
           |
       COMPILER
           |
         TEAVM
           |
       JAVASCRIPT
```

### Main Modules

| Module | Responsibility |
|---|---|
| `dejaview-core` | Core abstractions and framework foundation |
| `dejaview-reactivity` | Signals, Computed and Effects |
| `dejaview-renderer` | Rendering engine |
| `dejaview-dom` | Browser DOM integration |
| `dejaview-compiler` | Compilation pipeline |
| `dejaview-router` | Application routing |
| `dejaview-di` | Dependency Injection |
| `dejaview-http` | HTTP client |
| `dejaview-forms` | Reactive forms and validation |
| `dejaview-server` | Server-side platform support |
| `dejaview-ssr` | Server-Side Rendering |
| `dejaview-ssg` | Static Site Generation |
| `dejaview-hydration` | Client-side hydration |
| `dejaview-spring-boot` | Spring Boot integration |
| `dejaview-cli` | Command-line interface |
| `dejaview-devtools` | Development and debugging tools |
| `dejaview-testing` | Testing utilities |

The module structure is a proposed architecture and may evolve during development.

## Planned Features

### Component System

- Reusable components.
- Declarative UI.
- Component composition.
- Inputs and outputs.
- Slots.
- Lifecycle hooks.
- Dynamic components.
- Conditional and list rendering.

### Reactivity Engine

- Signals.
- Computed values.
- Effects.
- Automatic dependency tracking.
- Fine-grained updates.
- Batching and scheduling.
- Effect cleanup.

### Rendering

- Client-Side Rendering (CSR).
- Server-Side Rendering (SSR).
- Static Site Generation (SSG).
- Hydration.
- Streaming SSR.
- Selective hydration.
- Islands architecture.

### Application Framework

- Dependency Injection.
- Client-side routing.
- Route guards.
- Lazy loading.
- HTTP client.
- Reactive forms.
- Validation.
- State management.
- Internationalization.

### Developer Tooling

- DejaView CLI.
- Development server.
- Hot Reload.
- Production builds.
- Build optimization.
- Component inspector.
- Signal inspector.
- Rendering profiler.
- Testing utilities.

### Ecosystem

- Spring Boot integration.
- Component libraries.
- Plugin system.
- Third-party integrations.
- Community extensions.

> These capabilities are part of the project roadmap and should not be considered implemented until officially released.

## Example

The following example illustrates the intended component API. It is a conceptual API and is not yet a guaranteed working implementation.

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

## Project Status

**Early architecture and planning stage.**

DejaView is currently being designed. Its architecture, public APIs, module boundaries and implementation roadmap are subject to change.

There is no stable release yet.

### Roadmap

| Phase | Description | Status |
|---|---|---|
| 1 | Foundation and JavaScript runtime | Planned |
| 2 | Reactivity Engine | Planned |
| 3 | Component System | Planned |
| 4 | Application Framework | Planned |
| 5 | Compiler and Tooling | Planned |
| 6 | SSR and SSG | Planned |
| 7 | Advanced Features | Planned |
| 8 | Ecosystem | Planned |

The first development milestone is to compile a Java application into JavaScript, initialize the DejaView runtime and render an interactive component in the browser.

## Project Structure

```text
dejaview/
├── docs/
│   ├── ARCHITECTURE.md
│   ├── COMPILER.md
│   ├── RENDERING.md
│   ├── REACTIVITY.md
│   ├── COMPONENTS.md
│   └── ADR/
│
├── dejaview-core/
│   └── pom.xml
├── dejaview-reactivity/
│   └── pom.xml
├── dejaview-renderer/
│   └── pom.xml
├── dejaview-dom/
│   └── pom.xml
├── dejaview-compiler/
│   └── pom.xml
├── dejaview-router/
│   └── pom.xml
├── dejaview-http/
│   └── pom.xml
├── dejaview-forms/
│   └── pom.xml
├── dejaview-di/
│   └── pom.xml
├── dejaview-server/
│   └── pom.xml
├── dejaview-ssr/
│   └── pom.xml
├── dejaview-ssg/
│   └── pom.xml
├── dejaview-hydration/
│   └── pom.xml
├── dejaview-spring-boot/
│   └── pom.xml
├── dejaview-cli/
│   └── pom.xml
├── dejaview-devtools/
│   └── pom.xml
├── dejaview-testing/
│   └── pom.xml
│
├── examples/
├── benchmarks/
│
├── .mvn/
├── mvnw
├── mvnw.cmd
├── pom.xml
├── .gitignore
├── README.md
└── LICENSE
```

The project uses a **Maven multi-module architecture**, with a root POM managing shared configuration, dependencies and build plugins.

## Building the Project

DejaView uses [Apache Maven](https://maven.apache.org/) as its build system and dependency management tool.

The project includes Maven Wrapper to ensure a consistent Maven version across development environments.

### Requirements

- JDK 25 or compatible version defined by the project.
- Maven Wrapper (included in the repository).

### Build Commands

```bash
# Compile all modules
./mvnw clean compile

# Run tests
./mvnw test

# Package all modules
./mvnw clean package

# Install artifacts into the local Maven repository
./mvnw clean install

# Build a specific module and its required dependencies
./mvnw -pl dejaview-core -am package

# Skip tests
./mvnw clean package -DskipTests
```

On Windows, use `mvnw.cmd` instead of `./mvnw`.

### Maven Architecture

The root `pom.xml` is responsible for:

- Declaring all framework modules.
- Centralizing dependency versions through `dependencyManagement`.
- Managing plugin versions through `pluginManagement`.
- Defining shared Java compiler settings.
- Configuring common build properties.
- Maintaining consistent project metadata.

Individual modules declare only their own dependencies and specific build requirements.

## Documentation

Technical documentation will be maintained in the `docs/` directory.

| Document | Description |
|---|---|
| `ARCHITECTURE.md` | Overall framework architecture |
| `COMPILER.md` | Java compilation and build pipeline |
| `RENDERING.md` | Rendering strategies and hydration |
| `REACTIVITY.md` | Reactive engine internals |
| `COMPONENTS.md` | Component system and lifecycle |
| `ADR/` | Architectural Decision Records |

## Design Principles

DejaView is built around a few fundamental decisions:

1. Java is the primary language for application development.
2. JavaScript is the only browser compilation target.
3. The Core must remain independent of browser-specific APIs.
4. Reactivity should be granular rather than relying on unnecessary full-tree updates.
5. Components should be composable and independently manageable.
6. Rendering strategies should share a consistent component model.
7. Tooling and developer experience are first-class concerns.
8. Public APIs should remain consistent across framework modules.
9. Maven is the standard build and dependency management system.

## Contributing

DejaView is in its early development stage.

Contribution guidelines, development setup, coding standards and issue templates will be published as the project matures.

Before contributing to architectural components, please review the documentation and the corresponding Architectural Decision Records.

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
