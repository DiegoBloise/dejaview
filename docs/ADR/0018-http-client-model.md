# ADR-0018: HTTP client model

- **Status:** Accepted
- **Date:** 2026-11-12

## Context

Sprint 11 had to deliver `dejaview-http`: one consistent Java API through
which components and services consume external APIs from the browser, plus
the JSON plumbing any real client needs.

Constraints and preconditions:

1. The browser target is TeaVM-compiled; browser specifics must stay behind
   an adapter so JVM tests never touch them.
2. The browser is single-threaded: there is no blocking HTTP on the web
   platform. The API cannot mirror `java.net.http.HttpClient#send`.
3. `teavm-jso-apis` 0.16.0 ships typed `org.teavm.jso.ajax.XMLHttpRequest`
   bindings (`open`/`send`/`abort`/`onLoad`/`onError`/`onTimeout`/`onAbort`,
   `getStatus`/`getStatusText`/`getResponseText`/`getAllResponseHeaders`)
   but ships **no Fetch API** bindings and no `Headers`, `AbortController`
   or `ReadableStream` types. `JSPromise` exists, but its blocking
   `await()` mode is not usable for HTTP delivery in this design.
4. XHR has no typed `timeout` or `withCredentials` properties in the bindings;
   both are settable only through JS interop.
5. HTTP semantics (statuses as values vs as failures) had to be chosen;
   exception-as-control-flow does not survive async delivery.

## Decision

### Async-only API, exactly-once consumer delivery

`HttpClient.send(HttpRequest, Consumer<HttpResult>)` returns an `HttpCall`.
`HttpResult` is sealed: `Success(HttpResponse)` or `Failure(HttpFailure)`.
The consumer is invoked **exactly once**; `HttpClient` guards with an
atomic flag so even a misbehaving transport cannot double-deliver. Delivery
is synchronous when the transport delivers synchronously (fake transports in
tests; interceptor failures), otherwise on the browser event loop.
Consumer exceptions are not caught — consumers must not throw.

`HttpCall.cancel()` is best-effort and asynchronous; the consumer is still
notified once the transport settles the call (with `cancelled` when the
cancellation wins the race). `HttpClient` wraps the transport's call so
`isDone()` reflects delivery to the consumer, not merely transport
settlement.

### HTTP error statuses are `Success`; transport failures are `Failure`

A completed round-trip — any HTTP status — is `HttpResult.Success`;
`HttpResponse#ok()` is `true` for 200–299. `Failure` is reserved for
transport-level failures with stable codes: `network-error` (including
XHR status 0, which indicates an opaque or incomplete round-trip),
`timeout`, `cancelled`, `interceptor-error`, `missing-transport`,
`json-error`. This keeps control flow explicit without exceptions and
mirrors how the router reports denied navigations as values.

### The browser transport is XMLHttpRequest, not Fetch (deviation)

The sprint checklist names a Fetch adapter; this sprint ships an XHR
adapter instead. Fetch would require writing brand-new typed bindings for
`fetch`/`Response`/`Headers`/`AbortController` and re-plumbing cancellation
through promise interop, for zero user-visible gain: the XHR surface covers
every use case in scope (methods, headers, body, timeout, credentials,
cancellation, header reading) with fully typed APIs already in the
toolchain. The consequences that differ from Fetch and are documented here:

- **Cookies cannot be omitted on same-origin requests.** XHR always sends
  same-origin cookies. Credentials policies map as `OMIT → withCredentials
  = false`, `SAME_ORIGIN → withCredentials = false` (same-origin cookies
  ride along regardless), `INCLUDE → withCredentials = true` (adds
  cross-origin cookies/auth). Only `INCLUDE` is observable on the wire.
- **`timeout` and `withCredentials` are set via `@JSBody` bridges**, since
  the typed bindings lack those properties.
- **Response bodies are text** (`responseText`); there is no streaming.
- **Status 0 is not a status.** Onload with status 0 is reported as
  `network-error`, never as a response.

`dejaview-http` depends on `dejaview-core` only; TeaVM artifacts are
`provided`. JVM unit tests use fake transports and never load the browser
adapter (it is referenced solely inside `HttpClient`'s lazy factory, behind
`try/catch (Throwable)`, so its absence degrades to a delivered
`missing-transport` failure instead of an initializer crash).

### Effective defaults are resolved by the client, not the transport

Per-request `timeoutMillis`/`credentials` of `-1`/`null` ("inherit") are
resolved against the client's defaults inside `HttpClient.send`, so the
request handed to any `HttpTransport` carries concrete values. The JVM test
suite asserts that resolution through fake transports. A client default of
`-1` is passed through as disabled (browser mapping: XHR `timeout = 0`).
Client default credentials: `SAME_ORIGIN`. Client timeouts use
`long` milliseconds; the browser transport clamps to XHR's `int` ms range.

### Interceptors run in registration order; response interceptors see completed round-trips only

Request interceptors transform the outgoing request in registration order,
before the client resolves effective defaults. Response interceptors
transform the response in registration order, only for `Success` results
(HTTP error statuses included); transport failures bypass them. An
interceptor that throws, or returns `null`, fails the call with
`interceptor-error` and the original cause is preserved.

### Headers are immutable and case-insensitive

Names are stored lower-cased; insertion order is preserved; multi-valued
entries are kept as lists; on write (request) they join with `", "`.
Response headers are parsed from the raw `getAllResponseHeaders()` block
(split on CRLF/LF, first `:` separates name/value, names lower-cased, one
value per line). Values containing CR/LF are rejected at build time.

### JSON is dependency-free

`JsonValue` is a sealed tree (`JsonObject` insertion-ordered map,
`JsonArray`, `JsonString`, `JsonNumber` finite-only doubles, `JsonBoolean`,
`JsonNull`). Integers beyond 2^53 lose precision — documented. The parser
is recursive descent with a 512-level depth cap; duplicate object keys
collapse to the last value; errors carry the character offset and codec
code `json-error`; the writer rejects non-finite numbers and escapes
control characters. This avoids a reflective JSON dependency, which TeaVM
cannot use, and keeps the output small.

### Cancellation granularity is the whole call

There is no per-read cancellation; `cancel()` maps to `xhr.abort()` and the
`abort` event settles the call as `cancelled`. A call cancelled before it
started settles as `cancelled` immediately without touching the network.

## Consequences

- Components and services consume external APIs through one Java API, proven
  in Chromium by `ShowcaseBrowserIT#httpExampleFetchesPingJson` (`?example=http`
  fetches `ping.json` and renders `pong`).
- 67 JVM unit tests cover methods, headers, query encoding (`+` round-trips
  with the router's parser), interceptor order and failure, timeout and
  credentials resolution, cancellation, once-only delivery and JSON
  round-trips.
- The Fetch deviation is user-visible in one place only: none. The API
  surface is transport-agnostic; swapping XHR for a typed Fetch binding
  later does not change `dejaview-http`'s public contract.
- Async-only APIs will surprise ports from `java.net.http.HttpClient`;
  this is inherent to the web platform and documented rather than hidden.

## Alternatives considered

- **Hand-written Fetch/Promise/Headers JSO bindings now.** Rejected:
  doubles the interop surface for no functional gain in scope; the typed XHR
  bindings already cover everything required.
- **Synchronous-looking API via `JSPromise.await()`.** Rejected: blocks the
  browser event loop; misuses TeaVM's escape hatch; poor fit for reactive
  rendering.
- **HTTP statuses as `Failure`.** Rejected: conflates "the API said no"
  with "the network failed"; `ok()` is one branch instead of an exception.
- **A JSON library dependency.** Rejected: reflection-based (de)serialization
  is unavailable under TeaVM without extensive registration; the dependency-free
  tree is smaller, faster and explicit.
- **Streaming response bodies.** Rejected for this sprint (XHR cannot);
  revisited only if SSE/streaming use cases appear.
- **A `credentials: OMIT` promise that XHR cannot keep.** Rejected rather
  than faked; the limitation is documented here and on `HttpCredentials`.

## Revisit

When `teavm-jso-apis` (or maintained TeaVM extensions) ship typed Fetch /
`AbortController` bindings, the browser adapter can be swapped behind
`HttpTransport` without API changes. When server-driven rendering lands
(Sprints 16–19), a server-side transport (e.g. `java.net.http`) implements
the same `HttpTransport` for SSR data fetching. If cross-origin cookie
omission becomes a hard requirement, Fetch-with-bindings or a preflighted
proxy endpoint are the two honest paths; XHR cannot provide it.
