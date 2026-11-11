# ADR-0001: TeaVM as the initial Java-to-JavaScript compiler

- **Status:** Accepted
- **Date:** 2026-11-11
- **Decision owners:** DejaView architecture team

## Context

DejaView applications are written in Java and must run in a browser. The
project requires a compiler that consumes JVM bytecode and emits JavaScript.
TeaVM was proposed as the initial candidate, but its compatibility with the
project's Java baseline had to be validated before the architecture could
depend on it.

## Decision

Use **TeaVM 0.16.0** with the `JAVASCRIPT` target as the initial compiler.

The compiler is invoked by `teavm-maven-plugin` during the `package` phase of
browser-targeted modules. The Java language and bytecode level remains **25**.

## Validation evidence

A standalone probe compiled and executed code using records, sealed interfaces,
pattern matching, guarded patterns, text blocks, lambdas and `List.of`.

| TeaVM | Java 25 class files | Browser execution |
|---|---|---|
| 0.12.0 | rejected (`major version 69`) | not reached |
| 0.16.0 | accepted | passed in Chromium |

The browser probe created DOM elements, registered a click listener and updated
text content. Details are in `docs/COMPILER.md`.

## Consequences

- Browser modules must declare `teavm-classlib` and `teavm-jso-apis` with
  `provided` scope. TeaVM 0.16.0 also requires `teavm-classlib` as a plugin
  dependency.
- Only the TeaVM-supported subset of the JDK is available in browser code.
- JSO APIs are used for browser interop and are confined to the platform
  adapter.
- Upgrading TeaVM requires re-running the compatibility probe; it is not assumed
  to be a safe version bump.
- WebAssembly targets remain out of scope even though TeaVM supports them.

## Alternatives considered

- **GWT** — mature Java-to-JavaScript compiler, but its ecosystem and
  development model are tied to a different framework generation and JSO-style
  interop would be replaced by GWT-specific APIs.
- **JSweet** — transpiles Java to TypeScript/JavaScript with a different
  runtime model and weaker control over the generated runtime.
- **CheerpJ** — JVM-in-browser approach, not a JavaScript compilation target in
  the sense required by the project.
- **Hand-written JavaScript with a Java facade** — would not satisfy the
  Java-first requirement.

## Revisit when

- TeaVM introduces a breaking change that affects the framework's use of JSO or
  the class library.
- The project needs a compiler feature TeaVM cannot provide.
- A browser support requirement cannot be met by TeaVM's output.
