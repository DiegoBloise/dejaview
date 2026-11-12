# ADR-0011: Core application abstractions

- **Status:** Accepted
- **Date:** 2026-11-12

## Context

Sprint 01 gave the browser runtime its own lifecycle: `DejaViewRuntime` tracks
mounted trees, and `MountedDom` detaches nodes and listeners. Sprint 02 requires
platform-independent foundations that later features can share:

- an application context;
- framework-wide configuration;
- lifecycle contracts;
- resource disposal;
- framework exceptions;
- logging;
- internal argument checks.

Without these abstractions each module would invent its own ownership and
diagnostic conventions. `ReactiveScope` already duplicated the reverse-order,
failure-aggregating disposal algorithm that an application context also needs.

## Decision

Add the following to `dejaview-core`:

### FrameworkConfig

An immutable record with `applicationName`, `development` and an immutable
property map. Browser bootstrap options such as the container id stay in
`RuntimeConfig` in `dejaview-dom`; `FrameworkConfig` is platform-independent.

### ApplicationContext

A small lifetime owner exposing the configuration, a logger and a
`ResourceRegistry`. It is created with a factory, can register resources and
disposes them idempotently. It is not a dependency-injection container.

### ResourceRegistry

A shared disposal primitive:

- resources are closed in reverse registration order;
- disposal is idempotent;
- a failing resource does not prevent the others from closing;
- the first failure is rethrown with later failures suppressed;
- registering after disposal throws `IllegalStateException`.

`ReactiveScope` now delegates to this registry instead of duplicating the
algorithm. This makes `dejaview-reactivity` depend on `dejaview-core`, which
remains platform-independent.

### Lifecycle

An interface with no-op `onStart()` and `onStop()` callbacks. Disposal remains
`AutoCloseable`, so lifecycle phases and resource release stay separate.

### DejaViewException

A runtime exception with an optional stable diagnostic code and a preserved
cause. Framework code can wrap platform failures without losing information.

### Logger and Loggers

A minimal logging contract with levels and `noop()` / `system()` factories.
Framework modules do not depend on a logging backend.

### Preconditions

A small internal utility for consistent non-null and non-blank checks.

## Consequences

- Application and reactive lifetimes share one disposal implementation.
- Framework configuration, logging and errors no longer need module-specific
  variants.
- `dejaview-reactivity` gains a dependency on `dejaview-core`; the dependency is
  acyclic and keeps both modules platform-independent.
- The abstractions are deliberately minimal. There is no dependency injection,
  component ownership, structured logging backend or asynchronous lifecycle.
- `RuntimeConfig` and `FrameworkConfig` are separate because browser bootstrap
  options and application-wide settings have different owners.

## Alternatives considered

- **Duplicate disposal in each module.** Rejected: the algorithms had already
  diverged in error handling.
- **Put the registry in reactivity.** Rejected: the application context and
  future component scopes need the same primitive, and core is the shared
  foundation.
- **Full DI container now.** Rejected: Sprint 09 owns dependency injection.
- **Adopt SLF4J or `java.util.logging`.** Rejected for the browser target: a
  tiny contract keeps TeaVM output small and avoids backend coupling.
- **Single unified configuration type.** Rejected: browser bootstrap options
  would leak into platform-independent code.

## Revisit

When dependency injection, component ownership, structured logging or a second
platform adapter is introduced. The `ApplicationContext` surface is the main
type to extend at that point.
