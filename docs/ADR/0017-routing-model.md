# ADR-0017: Client-side routing model

- **Status:** Accepted
- **Date:** 2026-11-12

## Context

Sprint 10 had to deliver client-side navigation (`dejaview-router`): route
definitions, matching, path and query parameters, nested routes, outlets,
browser history, navigation events, guards, redirects, not-found routes and
route-level metadata.

Constraints and preconditions:

1. The browser target is TeaVM-compiled and the core carries no browser or
   TeaVM imports, so URL parsing and matching must be platform-neutral.
2. The component model (ADR-0014/0015) renders immutable view trees;
   component identity at one template position was positional, or swapped by
   class token (`componentFactory(Class, Supplier)`), or keyed in children
   lists. Single-position keyed views were always reused.
3. Browser history APIs (`pushState`, `popstate`) are TeaVM-only; JVM tests
   must not touch them.
4. SSR exists only as a later milestone (Sprints 16–19); the URL is a
   browser-shaped concept for now.

## Decision

### Resolution happens before history is touched

`navigate(url)` and `replace(url)` resolve the URL fully — matching,
redirects, guards — and only then call `history.push(url)` /
`history.replace(url)` and commit the match to the `Signal`. A denied or
unmatched navigation leaves the current match and the history stack
unchanged, returns `false` and emits `NavigationEvent.Cancelled`. Resolution
is synchronous, on the calling thread; listener exceptions propagate to the
caller.

### URLs are normalized once, centrally

`UrlParser` drops the fragment; collapses duplicate slashes; adds a missing
leading slash; strips a trailing slash (except root); treats blank as `/`;
and splits segments before percent-decoding, so `%2F` never acts as a
separator. The query string is split on `&` then on the first `=`; keys and
values decode with `+` as space; repeated keys are first-wins; `malformed
%XX` sequences are kept raw. Path `+` is a literal plus. The normalized URL
keeps `path?query`; the fragment never survives.

### Patterns: three segment kinds

`literal`, `:param` (one segment, captures the decoded segment) and `**`
(wildcard; last segment only; consumes the rest). Child routes nest under
their parent; an empty child path is the index route of its parent. Root
routes must start with `/` (or be `**`); child paths must not. A route
activates exactly one of a component factory or a redirect, never both. Built
routes are immutable; each route carries a JVM-unique id (`route:N`) used for
diagnostics and as the outlet's key.

### Redirects and guards, root-first

Route-declared redirects run root-first over the matched chain; guards run
root-first over the final chain with a prefix-accumulated
`NavigationContext` (parameters and metadata of levels `0..k`, chain up to
`k`, so a guard sees only what its own route has contributed). The first
non-`Allow` decision wins: `Deny(reason)` cancels the navigation; a
`Redirect(url)` (from a route or a guard) discards the chain and re-resolves
the target with a budget of ten hops, then fails with `redirect-loop`.
`guard-error` is thrown when a guard returns `null`. Guards on a redirecting
route are never evaluated — there is nothing to activate.

### Component identity: keys now swap at a single position

The outlet renders the active route's component keyed by the route id, and
the E2E test for Sprint 10 exposed that single-position keyed component
views were reused regardless of key: navigating from one route to another
rebound inputs onto the previous screen's component instance instead of
mounting the new screen. The reconciler now replaces the mounted component
when both views carry keys and the keys differ — the same identity rule the
keyed children lists already use. The class-token rule is unchanged: either
signal (token mismatch or key mismatch) triggers the swap; unkeyed tokenless
views still reuse positionally.

### Browser history is an adapter, not the router

`RouterHistory` is the seam: `current()`, `push`, `replace`, `back`,
`forward`, `listen`. `MemoryRouterHistory` (default; seeded stack; notifies
listeners from `back()`/`forward()`) serves JVM tests and non-browser
defaults. `BrowserHistory` (TeaVM-only) derives `current()` from
`location.pathname` + `location.search` (fragment dropped), calls
`pushState`/`replaceState`, and forwards `popstate` to listeners.
`push`/`replace` never notify listeners. Browser-initiated navigations are
adopted by re-resolving the restored URL without writing history; a guard
that denies such a navigation cannot revert the browser URL (the `popstate`
already happened), so the outlet keeps the previous match — a documented
limitation.

### The outlet renders one chain level

`RouterOutlet()` renders level 0; `RouterOutlet(router, level)` renders a
deeper level, so layouts embed the next outlet. Levels outside the matched
chain render an empty fragment. The route's path parameters are pushed as
inputs (`input("id", String.class)` for `/users/:id`). The outlet is keyed by
`route.id()`; navigation never reuses a component instance across routes.

### Navigation events

For each navigation the sequence is `Started` → `(Redirected)*` → `Ended` or
`Cancelled`. `Ended` fires only after the match is committed, so listeners
observing it see the new current match. `events()` returns a reversible
handle; exceptions in listeners propagate to the calling thread.

### Not-found routes

`Router.builder().notFound(route)` is matched as a normal chain — guards
included — when no root matches; if it matches, it activates, otherwise the
resolution is `no-match`. Registering a trailing `**` root is an equivalent
alternative (used by the showcase, so the example works under any static
path). Neither form throws: an unmatched navigation returns `false` and
emits `Cancelled` with reason `no-match`.

### Failure model

Stable `DejaViewException` codes: `invalid-route` (definition time: blank
path, `?`/`#` in a pattern, duplicate slash, empty segment, misplaced or
single-`*` wildcard, invalid or duplicated parameter name, a route
declaring both or neither of component/redirect, a redirect with children, an
absolute child path, a root not starting with `/`), `redirect-loop` (ten
hops exhausted), `guard-error` (null decision), `closed-router`,
`history-error`. A `null`/`NO_MATCH` resolution is not an exception.

### Lazy route loading is factory deferral only

TeaVM emits a single bundle; the current build has no code-splitting support,
so no route module is loaded on demand. What the router provides is
activation-time deferral: route components are built from factories when the
route activates, not when the route tree is defined. Bundle-level lazy
loading is deferred to Sprint 15 (production compiler) and is recorded as a
deviation in this sprint's notes; it is intentionally not implemented as a
pretend stub.

## Consequences

- A DejaView application is navigable without full page reloads, proven by
  the showcase's Chromium E2E test (`?example=router`): parameterized routes,
  navigation, browser back and forward.
- Guard semantics are explicit and asymmetric by necessity: router-initiated
  navigations discard the URL on denial; browser-initiated ones keep it.
- Keyed component replacement is a general renderer rule now, not a router
  special case — other single-position keyed component views get the same
  identity semantics.
- Route ids are JVM-unique counters (`route:N`); they identify components
  within a session, not across restarts, and must not be persisted.
- The injector is not wired to routes or outlets; route components receive
  dependencies through plain constructor parameters at the composition root.
- Non-browser consumers (tests, future SSG) must provide a `RouterHistory`;
  `MemoryRouterHistory` is the supported fake.

## Alternatives considered

- **Router reading `location` directly, no history abstraction.** Rejected:
  DOM-coupled core, untestable on the JVM.
- **Hash-based routing as the default.** Rejected: clean URLs are the
  baseline; a `HashHistory` adapter can come later without changing the
  router.
- **Throwing `no-match` instead of a `false` return.** Rejected: failed
  navigations are control flow, not exceptions; the events API carries the
  reason for diagnostics and history UIs.
- **Positional reuse regardless of keys (status quo before this ADR).**
  Rejected: rebound inputs of the previous screen onto the new route's view,
  visibly broken navigation; it contradicted the keyed-list identity rule.
- **`loadChildren`-style bundle splitting in this sprint.** Rejected: not
  supported by the single-bundle TeaVM output; belongs to Sprint 15.
- **Guards only at declaration site instead of root-first chain evaluation.**
  Rejected: parent guards (auth, shell) must dominate child routes; running
  them in declaration order per level keeps semantics unsurprising.

## Revisit

When SSR and hydration land (Sprints 16–19: server URL, base href,
per-request matches), when the production compiler can emit multiple bundles
(Sprint 15), when navigation guards need to be asynchronous (data prefetch),
or when the DI bridge creates per-view child injectors automatically.
