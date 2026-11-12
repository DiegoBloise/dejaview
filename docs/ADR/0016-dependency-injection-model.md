# ADR-0016: Dependency injection model

- **Status:** Accepted
- **Date:** 2026-11-12

## Context

Sprint 09 had to deliver a dependency injection system for DejaView:
`dejaview-di`, tokens, providers, singleton/transient/scoped bindings,
hierarchical injectors, an `inject()` API, circular dependency detection and
provider disposal.

Constraints and preconditions:

1. The browser target is TeaVM-compiled. Runtime classpath scanning and
   reflection are not assumed to work there, and DejaView core deliberately
   carries no browser or TeaVM imports.
2. The roadmap speaks of "constructor or field injection strategy", which left
   the mechanism open.
3. The component model (ADR-0014/0015) has no framework-owned wiring; DI had
   to be usable from plain constructors without entangling `dejaview-core`.
4. Disposal semantics already exist in `ResourceRegistry` and are the shared
   convention for `ApplicationContext` and `AbstractComponent`.

## Decision

### No reflection, no scanning — explicit providers

Bindings are written out by hand:

```java
InjectorBuilder builder = Injector.builder();
builder.bind(UserService.class)
       .to(injector -> new UserService(injector.inject(HttpClient.class)));
Injector root = builder.build();
```

`Provider<T>` is a functional interface (`Injector → T`). "Constructor
injection" is satisfied structurally: a factory calls the constructor with
resolved arguments. Field injection and annotations (`@Inject`, `@Singleton`,
`@Service`) do not exist in this sprint; they remain a proposal layered on this
surface, consistent with the programmatic-API-first decision for components.

### Tokens are identity-based; the class overload is canonical

`InjectionToken<T>` is identity-equal; `name()` is diagnostic only.
`InjectionToken.of(Class<T>)` returns one cached token per class, so
`bind(Foo.class)` and `inject(Foo.class)` always agree.
`InjectionToken.of(String)` produces a *distinct* token per call, which forces
a canonical token per dependency instead of string-based matching. Named tokens
are for same-type dependencies with different meanings.

### Three scopes, explicit ownership

| Scope | Cache | Ownership / disposal |
|---|---|---|
| `SINGLETON` (default) | one per registering injector | registering injector closes it |
| `TRANSIENT` | never cached | never closed by the injector; consumer owns it |
| `SCOPED` | one per injector that resolves the binding | resolving injector closes it |

SCOPED is the "request scope" of the web world: a child injector created per
request/view creates its own instance of the same binding, disposed when that
child closes.

### The provider receiver is the owning injector

`Provider.provide(Injector)` receives the injector that owns the instance it
produces: the registering injector for `SINGLETON`, the resolving injector for
`SCOPED` and `TRANSIENT`. Consequences: singleton factories see the root
registry, while request-scoped factories see the child scope and can depend on
sibling scoped bindings of that same scope.

### Hierarchy: children fall through, nearest wins, no cross-sibling leakage

`injector.child(String)` / `child()` create an empty child scope. Lookups walk
from the resolving injector upwards; a binding closer to the resolver wins.
Bindings can be added to a live injector via `injector.bind(...)`, which is the
supported way to shadow a parent binding in one scope (e.g. a test double for a
single request) without touching the parent or the siblings.

`build()` copies the registrations into the injector. Reusing a builder yields
independent injectors; adding a binding to a live injector does not mutate a
previously built injector.

### Disposal via `ResourceRegistry`

`AutoCloseable` instances produced by providers are registered on the owning
injector's `ResourceRegistry` (singleton → registering injector; scoped →
resolving injector) and closed in reverse creation order when that injector
closes. Children close before their parent, in reverse creation order.
`close()` is idempotent. `TRANSIENT` instances and constants bound with
`toInstance(...)` are never injector-disposed — closing is a statement of
ownership, and the injector only owns what it created for caching.

### Cycle detection on a thread-local resolution path

Before any instance is created or cached, the resolution path (token identity
on a thread-local stack) is checked. Re-entrant resolution of an in-flight
token throws `DejaViewException("circular-dependency", "...A -> B -> A")`
before any cache is mutated, so a failed resolution leaves the injector in a
consistent state. Detection works across injector boundaries because the path
is thread-local, not per-injector.

### Failure model

Stable codes on `DejaViewException`: `missing-provider` (names the token, the
injector, and the in-flight chain when nested), `circular-dependency`,
`provider-error` (wraps non-`DejaViewException`s with the cause preserved; a
`DejaViewException` from user code passes through unchanged),
`duplicate-binding` (fail-fast at binding time, local to one injector),
`closed-injector`.

Injectors are lazy: nothing is instantiated until resolved. Injectors are not
thread-safe; the target runtimes are the single-threaded browser and JVM
tests. The component system is not yet wired to the injector: components
receive dependencies through plain constructor parameters, resolved at the
composition root before mount.

## Consequences

- DI is TeaVM-safe by construction: the whole module compiles without
  reflection APIs and every dependency edge is visible at the call site.
- Wiring is verbose by design; there is no "automagic" to debug at runtime.
- Missing bindings fail at resolution time with a message naming the token,
  the injector and the in-flight chain.
- Scope mistakes are impossible to hide: singleton vs request-scoped instances
  are observably different objects, and disposal ownership is explicit.
- Duplicate binding fails at bind time, not at first resolution.
- The class-token identity approach means class-based wiring is unambiguous
  but also means named tokens must be kept as fields/constants; ad-hoc
  `InjectionToken.of("x")` inline calls create distinct tokens.

## Alternatives considered

- **Reflection/classpath scanning with `@Inject` constructors.** Rejected:
  not TeaVM-safe, hides the wiring graph, and contradicts the
  programmatic-first surface chosen for components.
- **String-based token lookup.** Rejected: silently ambiguous; identity +
  class-canonical tokens make duplicates fail fast and class wiring
  interoperable with explicit tokens.
- **Supplier-only injection without scopes.** Rejected: the roadmap requires
  singleton/transient/scoped semantics with defined disposal.
- **Global root-only injector.** Rejected: scopes are the primitive for
  request/view-lifetime services; a global always leaks between scopes.
- **Eager resolution at `build()`.** Rejected: lazy resolution keeps startup
  cheap, keeps missing-provider failures local to the resolution path, and
  keeps `build()` allocation-only.
- **`toInstance` instances closing with the injector.** Rejected: a constant
  handed in by the application is owned by the application; injector disposal
  must reflect injector creation.

## Revisit

When the annotation processing milestone is specified (field injection and
qualifiers become possible), when `validate()`-style eager startup checks are
required, when injectors must be thread-safe (multi-threaded SSR), or when DI
needs a first-class bridge to the component lifecycle (e.g. per-view child
scopes automatically created by `DomRenderer`).
