# ADR-0023: Server platform abstraction and `dejaview-server`

- **Status:** Accepted
- **Date:** 2026-11-13

## Context

Phase VI (Sprint 16) prepares DejaView for server-side execution — the
foundation for Server-Side Rendering (Sprint 17), Static Site Generation
(Sprint 18) and hydration (Sprint 19). The acceptance criterion is: the
framework must have a server platform foundation capable of supporting SSR.

Constraints and preconditions:

1. `dejaview-core` must stay platform-neutral; it already holds no reference
   to browser APIs and must not grow one (ADR-0003, ADR-0005).
2. The server side must not depend on any servlet, HTTP framework or concrete
   server — Spring Boot integration is Sprint 21 (Phase VIII) and would be
   premature here.
3. Browser-only components must never be mounted on the server by accident,
   and the failure mode must be explicit and loud.
4. State must not leak between requests served by the same process.
5. Whatever crosses the boundary as JSON must be serializable by
   construction; silent coercion (the `JsonValue.ofJava` behavior of mapping
   unknown values to `null`) corrupts output invisibly.

## Decision

### A dedicated `dejaview-server` module, pure Java 25

New module `dejaview-server` (depends on `dejaview-core`, `dejaview-di`,
`dejaview-state`, `dejaview-http`; zero browser APIs, zero TeaVM). The module
is therefore compilable and fully testable on the JVM, and a
`ModulePurityTest` fails the build if the DOM adapter or TeaVM ever lands on
its classpath.

### Platform contracts: `PlatformKind`, `Platform`, `ServerPlatform`

`PlatformKind` (`BROWSER`, `SERVER`), `Platform` (process-lifetime value
exposing `kind()`), and `ServerPlatform` (stateless singleton
`ServerPlatform.INSTANCE`). Binding goes through
`ServerTokens.PLATFORM` = `InjectionToken.of(Platform.class)`;
`ServerBindings.apply(InjectorBuilder)` registers the platform instance plus
`StateStorage` (see below). Browser code keeps its own platform binding when
one is needed — this sprint does not touch the browser runtime.

### Request scope: `ServerContext`

One context per request, created from the host root injector:

- a child injector named `server.request`; bindings made on it shadow the
  root and never leak into it — `ServerTokens.REQUEST` and
  `ServerTokens.CONTEXT` are bound there as `toInstance` constants;
- an owned `ResourceRegistry` disposed in reverse registration order on
  `close()` (idempotent); the child injector is closed after the registry
  (children close before parents, per the DI contract);
- an owned `PlatformCompatibilityRegistry` (no global state).

State isolation out of the box: if the parent injector cannot resolve
`StateStorage`, the context binds a fresh `MemoryStateStorage` on the child.
`ServerBindings.apply` binds `StateStorage` with `Scope.SCOPED` — one
`MemoryStateStorage` per resolving injector, i.e. per request — and hosts
needing durable/distributed state bind their own instead. `toInstance` is not
used for storage precisely so the root is not silently shared across requests.

### Component compatibility: `PlatformCompatibility` and the registry

`PlatformCompatibility` is `UNIVERSAL` (default), `BROWSER_ONLY`, or
`SERVER_ONLY`. Classification order, per context:

1. exact-class registration in the context's `PlatformCompatibilityRegistry`;
2. the `BrowserOnlyComponent` marker interface, by assignability;
3. `UNIVERSAL`.

`SERVER_ONLY` round-trips through the registry and is allowed to render on
the server; browser-side enforcement is a later concern. The marker exists
for application code, the registry for third-party components that cannot be
edited.

### Server rendering: `ServerRenderer` over a sealed `ServerNode` tree

`ServerRenderer.render(view, context)` walks the immutable `View` tree and
produces a `ServerTree`:

- elements become `ServerElement` (tag name, `Attribute`s, `Property`s and
  children — verbatim, no attribute interpretation; event handlers are
  dropped and reported, see below);
- text becomes `ServerText`; fragments splice their children into the parent —
  no wrapper node is emitted, and a multi-node root is wrapped in
  `ServerFragment`;
- components mirror the browser mount pipeline in shape, not in output:
  create → classify → (`BROWSER_ONLY` → `DejaViewException`
  `browser-only-component`, before `bind`/`onInit`/`render`) → `bind` →
  `onInit` → `render` exactly once → splice the produced view. When the view
  carries an explicit `componentType` token, classification happens before
  construction; otherwise right after creation. Either way the lifecycle never
  starts for a rejected component.
- every created component — including ones rejected after construction —
  receives `onDestroy()` (only if `onInit()` completed) and then `close()`, in
  reverse creation order, on the success and failure paths alike. Failures
  are collected and attached as suppressed/thrown causes, never swallowed.
- element event handlers are dropped (handlers are not serializable) and the
  dropped event type names are reported de-duplicated in first-seen order on
  `ServerTree.droppedEvents()`.

### Serialization boundary: `SerializationBoundary`

Strict `toJson`/`write`: `null`, `JsonValue` trees, `String`, `Boolean`,
finite `Number`s (NaN/±Infinity rejected), `Map<String, ?>` (non-string keys
rejected) and `List<?>`; nesting depth capped at
`SerializationBoundary.MAX_DEPTH` = 512. Anything else throws
`DejaViewException("not-serializable", ...)`. `JsonValue.ofJava` was rejected
for this boundary because it coerces unknown values to `null` silently.
Numbers are doubles at the JSON layer (integers above 2^53 lose precision);
the boundary documents this rather than pretend otherwise.

### Transport-neutral request/response values

`ServerRequest` (method, path with optional query string, `HttpHeaders`,
text body) and `ServerResponse` (status 100–599, `HttpHeaders`, text body,
`text/html; charset=utf-8` factory). They carry no socket/servlet types and
are not wired to any HTTP server; the SSR module and later host adapters
consume them.

## Consequences

- Sprint 17 can implement HTML serialization without revisiting module
  boundaries: render into a `ServerTree`, then serialize; per-request state
  and injector scoping are already in place.
- The framework gains its first server-side proof of the core abstractions:
  components, lifecycle, DI and `View` all run on the JVM unchanged.
- `dejaview-server` is not yet wired into any deployable; there is no HTML
  output yet and nothing is user-visible in an application.
- Browser applications do not bind `PLATFORM`; the token is consumed by
  server code only.

## Alternatives considered

- **Server code inside `dejaview-core`.** Rejected: core would grow
  request/response types and server lifecycle, violating the platform-neutral
  core invariant.
- **Platform as a core `ApplicationContext` service.** Rejected: `core` does
  not depend on `dejaview-di` (that would invert the dependency direction,
  `dejaview-di` depends on core); a `Platform` injectable belongs to the
  server module.
- **Global (static) compatibility registry.** Rejected: state leaks between
  requests and tests; per-context registries isolate both.
- **Rejecting browser-only components via module dependency absence.**
  Impossible: component trees are built dynamically; classification must be a
  runtime decision at mount time.
- **Silently dropping browser-only components from the tree.** Rejected: a
  missing section in the SSR page is far harder to diagnose than an explicit
  error naming the component class.
- **`JsonValue.ofJava` at the boundary.** Rejected (see above); strictness is
  the point of a boundary.
- **Annotations (`@BrowserOnly`) with an annotation processor.** Deferred:
  the programmatic marker is reflection-free and works today; the annotation
  family remains a proposal (see `docs/IMPLEMENTATION_STATUS.md`).

## Revisit triggers

- Sprint 17 (SSR) may extend the tree with attributes that must be
  HTML-escaped — serialization stays in `dejaview-ssr`, not in the tree types.
- Sprint 21 (Spring Boot) may require streaming `ServerResponse` bodies or
  servlet adapter types; those belong in the adapter module, not here.
- If state hydration needs typed values across the boundary, integrate
  `StateCodec` with the boundary rather than widening the accepted set.
