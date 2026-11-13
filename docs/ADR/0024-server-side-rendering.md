# ADR-0024: Server-side HTML rendering and `dejaview-ssr`

- **Status:** Accepted
- **Date:** 2026-11-13

## Context

Phase VI (Sprint 17) turns the server platform foundation of Sprint 16
(ADR-0023) into the first user-visible server capability: an application must
be able to generate its initial HTML on the server. Later sprints build on it —
Static Site Generation (Sprint 18), hydration (Sprint 19) and a deployable
server adapter such as Spring Boot (Sprint 21).

Constraints and preconditions:

1. `dejaview-server` already renders a `View` tree into a sealed
   `ServerNode` tree (`ServerElement`, `ServerText`, `ServerFragment`) with no
   browser APIs. HTML must be produced from that tree, not from a second
   renderer.
2. `dejaview-core` stays platform-neutral, and `dejaview-ssr` must be pure JVM
   code with zero browser and zero TeaVM imports (ADR-0003, ADR-0023).
3. Anything that can inject text — application data, request parameters,
   component output — must never become markup.
4. A host must be able to render without knowing the tree types, and a single
   failing component must be containable without failing the whole page.
5. The module must not invent a transport: rendering returns a value, and a
   `ServerResponse` is built from it for whatever HTTP host the application
   uses.
6. Hydration, state serialization and progressive streaming are Sprint 19+
   concerns and must not be faked here.

## Decision

### A dedicated `dejaview-ssr` module, depending on `dejaview-server`

`dejaview-ssr` depends only on `dejaview-server` (and transitively on core, DI,
state and HTTP). It is pure Java 25, has no browser or TeaVM imports, and a
`ModulePurityTest` fails the build if either ever lands on its classpath. It is
not a new `View` subtype and it changes no core contract: SSR is a pipeline over
the existing model.

### Pipeline: render, validate, serialize

`SsrRenderer` is the public entry point and runs three steps:

1. `ServerRenderer.render(view, context)` mounts the tree (Sprint 16
   semantics: one lifecycle pass, events dropped, components closed LIFO,
   browser-only components rejected);
2. `SsrValidator` accepts or rejects the tree and collects warnings;
3. `HtmlPage` (document) or `HtmlSerializer` (fragment) writes escaped HTML.

`renderPage(view, context, meta)` returns a complete document;
`renderFragment(view, context)` returns a fragment with no wrapper; the `Writer`
variants (`writePage`, `writeFragment`) write the same output to a sink. The
result is always an `SsrResult` — status, HTML, dropped events, boundary
reports, warnings and the attached failure — and `SsrResult.toResponse()`
produces a transport-neutral `ServerResponse.html(status, html)`.

### Validation is the security boundary

`SsrValidator` rejects a tree with `DejaViewException`
`ssr-validation-failed` and a message listing every violation when:

- a tag name does not match `^[a-z][a-z0-9-]*$`;
- an attribute name does not match `^[A-Za-z][A-Za-z0-9_.:-]*$`;
- a void element (`br`, `img`, `input`, …) has children;
- an element carries the same attribute twice;
- nesting depth exceeds `MAX_DEPTH` = 512.

The serializer never creates markup: tag and attribute names are emitted
verbatim, so validation is what makes that safe. Text is escaped (`&`, `<`,
`>`); attribute values add `"` and `'`. `serialize`/`write`/`render` validate
the whole tree before writing, and `SsrRenderer` validates once and then calls
the package-private `writeValidated` so a page is never half-written. Malformed
input fails; it is never silently repaired.

### Lossy but safe situations become warnings

An `SsrResult` can succeed and still carry `SsrFinding`s, collected in
validation order:

- `raw-text-escaped` — text inside `script`/`style` is escaped, so the browser
  renders it as text. Emitting it raw would be a code-injection hole.
- `skipped-property` — a DOM property outside the supported subset is not
  serialized.
- `empty-root` — the render produced no content.

Warnings never change the HTTP status. The property subset is deliberate: the
`value` property becomes the `value` attribute, `checked`/`selected`/
`disabled`/`readonly`/`multiple` become boolean attributes only when their value
is `"true"` (case-insensitive), an explicit attribute wins over a projected
property of the same name, and everything else (`className`, `innerHTML`,
`tabIndex`, …) is skipped and reported. A later sprint can widen the subset
without changing the contract.

### Failure model: generic 500, details stay in the host

An unhandled render failure or a validation failure produces `SsrResult` with
status `500` and a fixed generic document (`GENERIC_ERROR_HTML`) that names the
status only; the exception is attached to the result for the host to log and
never leaked to the browser. `isSuccess()` means `error == null`, so a host that
chooses a `404` status for its own not-found page still has a successful render.
Boundary containments and warnings do not make a render fail.

### Error boundaries for server rendering

Server rendering cannot "unmount and recover" in the browser's sense: the
response is the tree. `dejaview-server` therefore defines
`ServerErrorBoundary extends Component`: the protected subtree is whatever the
boundary's `render()` returns (typically its slot). When anything created inside
that subtree throws — while binding, initializing, rendering or producing a
subtree — the renderer:

1. destroys and closes the failed subtree only, in reverse creation order, so
   its resources are released even though the request continues;
2. records one `BoundaryError(boundaryType, errorType, message)` — the original
   failure class, not the normalized wrapper — in
   `ServerTree.boundaryErrors()`;
3. renders `fallback(error)` in place of the failed subtree, where `error` is
   the normalized `DejaViewException` (`server-render-failed` when the cause was
   not already a `DejaViewException`).

The boundary itself stays mounted and is destroyed/closed with the rest of the
render. A fallback that throws propagates to the next enclosing boundary, which
records its own entry; with no enclosing boundary the render fails. Nested
boundaries report only the innermost containment. `ServerTree` uses the
canonical `(root, droppedEvents, boundaryErrors)` constructor (the two-argument
form remains for callers without boundaries) and `isComplete()` is false when
anything was dropped or contained.

### Per-request metadata: `PageMeta` and `HtmlPage`

Metadata is request-scoped — the same component factory can render a different
title per request — so it cannot live on the immutable view tree. The host
creates one mutable `PageMeta` per request (`lang`, `title`, `description`,
`canonical`, `meta(name, …)`, `metaProperty(…)`; fluent setters) and components
write it while rendering. `HtmlPage` then emits a fixed, JavaScript-free
document skeleton: doctype, `<html lang>`, charset, viewport, optional title,
description and canonical link, the added `meta`/Open Graph tags in insertion
order, `<body>` with the serialized tree, and closing tags. Empty optional
values omit their tags; every value is escaped.

### Demo: `examples/ssr`

`examples/ssr` is an actual server-rendered application, not a snippet: plain
components, a GET search form, per-request `PageMeta`, a contained failing
widget, a `404` page, and a `ServerContext` plus root injector per request. It
is served by the JDK `HttpServer` on an ephemeral/loopback port
(`SsrDemoMain`, `-Dssr.port`) and covered by JVM tests plus a Playwright test
that runs with **JavaScript disabled** — the point being that the page works
without a single line of client code. It is JVM-only and has no TeaVM build.

## Consequences

- The acceptance criterion is met: a DejaView application generates its initial
  HTML on the server from the same component model the browser uses.
- The module composes the Sprint 16 contracts instead of duplicating them;
  serialization, validation and boundaries stay out of `dejaview-server` and
  `dejaview-core`.
- SSR is usable today by an application that owns its HTTP adapter; no
  framework-specific server is imposed.
- The output is a complete page, not a hydrated application. There is no state
  serialization, no hydration markers, no progressive streaming, and the
  `Writer` variants buffer the document before writing.
- Only the `value` and boolean property subset is representable; the rest is
  skipped with a warning.
- `script`/`style` content is escaped, which makes those elements safe but
  functionally inert when produced from text nodes.
- The website publishes the capability (15th feature card, guide section 20 in
  both languages) and the example is validated in Chromium.

## Alternatives considered

- **A new `View` subtype or a browser-like renderer on the server.** Rejected:
  `View` is sealed, and the server already has a tree; HTML is a serialization
  of it.
- **Serializing directly from `View` without a validated intermediate tree.**
  Rejected: it duplicates the component lifecycle and makes escaping a
  per-callsite responsibility instead of a single boundary.
- **Writing tags and attribute names without validation.** Rejected: a tag name
  is data until it is checked; allowing arbitrary names would turn the
  serializer into an injection vector.
- **Escaping `script`/`style` content silently.** Rejected: silent behavior is
  worse than a warning that names the element.
- **Streaming the document while rendering.** Deferred: resume-on-error and
  hydration need a different pipeline. Buffering first gives all-or-nothing
  responses, which is the safer default for this milestone.
- **Reflection or annotation scanning for metadata.** Rejected: the framework
  is reflection-free; `PageMeta` is explicit and request-scoped.
- **A servlet/Spring adapter in this module.** Rejected: Sprint 21 owns host
  integration; this module stays transport-neutral.

## Revisit triggers

- Sprint 18 (SSG) needs batch rendering of many routes and asset copying; those
  belong in a build-time pipeline, not in `SsrRenderer`.
- Sprint 19 (hydration) will need serialized state and stable markers in the
  emitted HTML; the serializer and `PageMeta` are the extension points.
- Sprint 21 (Spring Boot) may need streaming responses or servlet types; those
  belong in the adapter module.
- Widening the property subset (or supporting raw `script`/`style` content with
  an explicit opt-in) is a contract change and must update `HtmlRules`,
  `SsrValidator` and the tests together.
