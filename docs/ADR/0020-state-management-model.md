# ADR-0020: State management model — stores, async values, persistence

- **Status:** Accepted
- **Date:** 2026-11-13

## Context

Sprint 13 had to deliver `dejaview-state`: application-level state management
built on `dejaview-core` and `dejaview-reactivity`, so applications can share
state across components, derive values from it, await asynchronous operations
and persist state — without introducing global coupling.

Constraints and preconditions:

1. The module must stay platform-neutral for its contracts: `StateStore`,
   `AsyncStore`, `PersistedStore` and friends import no TeaVM or browser
   types. The only browser-aware file, `internal/BrowserStorage`, is compiled
   against TeaVM's JSO APIs (`provided` scope) and confined to `internal/`.
2. The browser is single-threaded; reactive updates are synchronous outside an
   explicit `Reactive.batch`.
3. `Signal<T>` disallows `null`, so async state must always be a concrete
   value — an explicit status plus optional payload, not a nullable field.
4. Persistence must be opt-in and must never crash the application:
   unavailable storage, quota errors and corrupt payloads are expected
   browser conditions, not exceptional ones.
5. Asynchronous completions can arrive in any order. A superseded response
   must never overwrite newer state or resurrect disposed stores.
6. DejaView has no global singletons by design (ADR-0016). Application-level
   state follows the same rule: stores are ordinary objects created at the
   composition root and passed down.

## Decision

### `StateStore<T>`: a shared, observable holder

A store owns a `Signal<T>`; `state()` is a tracked read; `set`/`update` write
synchronously and reject `null`. `select(Function|Selector)` returns a lazy
`Computed` owned by the store — `dispose()` disposes those computeds and then
makes `state()`/`set`/`select` throw `IllegalStateException`. Stores expose a
stable optional `name()` for diagnostics (`StateStore.named(...)`) and an
`initialState()` plus `reset()` for predictable local resets.

### `Selector<S, R>`: named projectors, no equality short-circuit

A `Selector` is a named, reusable projector (`cart/itemCount`, …). Results
compare with `Objects.equals` wherever equality matters; DejaView does not
short-circuit computeds on equal results yet (the Phase 2 refinement is
tracked in `docs/ROADMAP.md`). Selectors change nothing about evaluation —
naming is a diagnostics affordance.

### `Stores.combine`: cross-store derived values, no global store

`Stores.combine(left, right, combiner)` returns a `Computed` that reads both
stores; nesting produces further projections. The combined computed is **not
owned** by either store; callers dispose it (or wrap it in a `ReactiveScope`)
when the owning view is torn down. Cross-store projections are local by
construction, which is how DejaView composes shared state without a global
store of everything.

### `StateRegistry`: anti-global composition-root registry

An ordinary object created next to the injector/root component, keyed by name,
with lazy creation via `getOrCreate(name, supplier)` and reverse-order
`disposeAll()`. Because generic parameters are erased, the registry cannot
verify the state type per key; callers must use a consistent type per key
(the same rule as token-based DI elsewhere in DejaView). This is documented,
not enforced.

### `AsyncStore<T>`: exactly-once completions, stale-attempt rejection

One `Signal<AsyncState<T>>` per operation. `load()` increments a generation
counter and flips the status to `LOADING`; callbacks from earlier attempts
are silently dropped, and a second callback for the same attempt is dropped
too (settled-attempt guard), so exactly one outcome wins per attempt. A
loader that throws is caught and converted to
`AsyncError.of("load-error", ...)`. `Failure` keeps the previous value
(stale-while-revalidate), as does `LOADING` while refreshing.
`dispose()` invalidates pending attempts; their completions become no-ops;
calling `load()` after disposal throws.

### `AsyncState<T>` / `AsyncStatus` / `AsyncError`: statuses as values

`AsyncState` is immutable: `IDLE → LOADING → SUCCESS | FAILURE`, carrying an
`Optional<T> data` (present on success, kept during refresh and after a
failed refresh) and an `Optional<AsyncError>` (present only on failure).
`AsyncError` is a record with a stable, machine-readable `code` (blank codes
rejected); `message` is diagnostic detail, never user-facing copy.
Applications map codes to localized messages, exactly like form error keys
(ADR-0019).

### Persistence: `StateStorage` + `StateCodec` + `PersistedStore`

- `StateStorage` is a minimal string-keyed contract (`read`, `write`,
  `remove`). Routine failures are thrown as unchecked exceptions and **caught
  by `PersistedStore`** — adapters need no ceremony.
- `StateCodec<T>` encodes/decodes strings; failures throw
  `StateCodecException` (wrapped by `StateCodec.of(...)` factories). Built-ins:
  `StateCodec.string()` and `StateCodec.integer()`.
- `PersistedStore<T>` restores the delegate store **before any observer
  exists** (no write-echo during restore). Afterwards a write-through `Effect`
  persists every subsequent change; its first run still reads the state (to
  register the dependency) but skips the write so an unreadable present entry
  is never clobbered. Failures are values:
  - `decode-error` — corrupt payload removed, initial state kept;
  - `storage-error` — unreadable storage, initial state kept;
  - `storage-write-error` — in-memory state unaffected; the next change retries.

  `restored()`, `restoreError()` and `writeError()` expose the outcome.
  `dispose()` detaches the write-through and keeps the stored value.
- `internal/BrowserStorage` adapts `localStorage`/`sessionStorage` through
  TeaVM JSO. When storage is unreachable every operation throws
  `IllegalStateException`, which `PersistedStore` maps to the codes above.
  JVM tests use `MemoryStateStorage`.

## Consequences

- `dejaview-state` is a new pure-Java module depending on `dejaview-core` +
  `dejaview-reactivity` (plus `provided` TeaVM JSO APIs for the browser
  adapter); 33 JUnit tests cover stores, selectors, composition, disposal,
  async lifecycle, stale-attempt rejection, exactly-once completions,
  persistence restore/write-through/failure paths and the registry.
- The showcase ships `StateDemo` (`?example=state`) — shared cart store,
  named selector projections, a cross-store discount, an async checkout
  (empty cart fails with code `empty-cart`) and a `localStorage`-persisted
  theme — verified by `ShowcaseBrowserIT#stateExampleManagesSharedDerivedAndAsyncState`
  in Chromium, including restore-across-reload.
- The website gains a bilingual "State management" feature card and tutorial
  section (sections renumbered to 15–19 in both languages), a state playground
  example and `WebsiteBrowserIT` coverage (cards 10→11, examples 9→10).
- Persistence is never implicit: nothing is read from or written to storage
  unless an application builds a `PersistedStore`.
- Async callbacks never run after disposal; an in-flight checkout is
  invalidated by `onDestroy`, matching the forms disposal semantics.

## Alternatives considered

- **Redux-style single global store.** Rejected: forces global coupling and a
  reducer ceremony Java doesn't need; independent stores plus `combine` cover
  shared projections without a singleton.
- **Reusing the DI injector to hand out stores.** Deferred: stores are plain
  constructor-injected objects; a `Provider<StateStore<...>>` wrapper is
  possible later as sugar, but baking it into `dejaview-di` now adds
  coupling without semantics.
- **Throwing persistence errors.** Rejected: mirrors ADR-0018/0019 —
  failures are values (`restored()`/`restoreError()`/`writeError()`), because
  storage is an unreliable, application-visible resource.
- **Promise-based async loader.** Rejected: TeaVM 0.16.0's typed bindings
  cover XHR and timers; a completion-callback contract with a generation
  counter is the whole contract, testable on the JVM.
- **IndexedDB acceptance now.** Deferred: `StateStorage` permits an adapter
  later; Sprint 13 ships the synchronous string contract that `localStorage`
  and `sessionStorage` implement.
- **Suspense/loaders for routes.** Out of scope: routing concerns belong to
  the router (ADR-0017); Sprint 13 delivers the state primitives a loader
  would be built on.
- **Equality short-circuit in `Computed`.** Deferred to the Phase 2
  refinement (see `docs/ROADMAP.md`); documented in `Selector` javadoc.

## Revisit

When the Phase 2 equality short-circuit lands, `select()` projections become
free to over-notify-less. If applications need structured persistence beyond
strings, an `IndexedDBStateStorage` or a typed `StateCodec` registry can be
proposed without changing the restore-before-observers contract. If the
reactivity engine gains topological ordering (see ADR-0010), `Stores.combine`
graphs could notify in topological order as well.
