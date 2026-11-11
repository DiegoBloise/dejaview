# ADR-0009: Lazy, cached computed values with push-based invalidation

- **Status:** Accepted
- **Date:** 2026-11-11

## Context

ADR-0008 introduced `Signal<T>` and a synchronous `Effect` as the first
reactivity slice and explicitly deferred computed values. Applications routinely
derive state from other state (doubled counters, filtered lists, formatted
labels). Recomputing such derivations in every effect duplicates logic and
re-executes work when multiple observers read the same derivation.

The full target engine also includes batching, scheduling, ownership scopes and
equality short-circuiting. Implementing all of them together would expand the
review surface and mix dependency-graph design with renderer integration.

## Decision

Add `Computed<T>` to `dejaview-reactivity` with these semantics:

1. **Lazy evaluation.** The supplier does not run on construction. It runs on the
   first `get()` and thereafter only when the cached value is dirty.
2. **Cache.** Repeated `get()` calls without an intervening invalidation return
   the cached value and do not re-run the supplier.
3. **Push-based invalidation.** When a dependency changes, the computed is
   marked dirty and its observers are notified immediately. Evaluation stays
   pull-based: the supplier runs on the next read, not during invalidation.
4. **Dependency re-collection.** Dependencies are cleared and collected again on
   every recomputation, so conditional reads work and stale dependencies are
   dropped.
5. **Computed-on-computed graphs.** A computed may read other computeds. The
   shared internal `ReactiveSource` / `ReactiveObserver` abstraction lets signals
   and computeds be observed uniformly without exposing observer management in
   the public API.
6. **Cycle detection.** A computed that is asked to evaluate while it is already
   computing throws `IllegalStateException` instead of recursing until the stack
   overflows.
7. **Failure recovery.** If the supplier throws, dependencies collected during
   the failed run are released and the computed stays dirty so a later `get()`
   can retry.
8. **Null rejection.** A supplier that returns `null` fails with
   `NullPointerException`, consistent with `Signal`.

The public API is `new Computed<>(() -> ...)`; the factory form `computed(...)`
shown in the architecture document is still a proposal.

## Consequences

- Derived state can be shared: several effects reading the same computed trigger
  a single computation per invalidation cycle.
- A computed notifies its dependents as soon as a dependency changes, even
  before it recomputes. An effect that reads the computed and observes an equal
  recomputed value still reruns. This over-execution is accepted for this slice
  and must be documented; equality short-circuiting requires evaluating during
  invalidation or deferring dependents, which belongs with the scheduler.
- `Computed` has no `dispose()` and no owner tree. A long-lived computed keeps
  its dependency registrations, as effects do until disposed. Ownership/scopes
  are a later Phase 2 slice.
- Computeds, like signals and effects, are not thread-safe and target the
  single-threaded browser runtime and single-threaded JVM tests.
- The reactivity module stays independent of `dejaview-core`, `dejaview-dom` and
  TeaVM.

## Alternatives considered

- **Eager recomputation on invalidation.** Rejected for this slice: it runs the
  supplier even when nobody reads the value and couples evaluation timing to
  dependency notification. Lazy evaluation is the smaller, more predictable
  behavior.
- **Recompute on every `get()` without caching.** Rejected: it defeats the
  purpose of derived state and multiplies work for multiple observers.
- **Equality short-circuit before notifying dependents.** Rejected for now: it
  requires eager evaluation or a scheduler-aware two-phase invalidation. Recorded
  as follow-up work rather than silently approximated.
- **Put computed values in `dejaview-core`.** Rejected for the same reason as
  ADR-0008: reactivity is a distinct responsibility and must stay usable without
  the view model.
- **Add `dispose()` to `Computed` immediately.** Rejected for this slice: without
  an ownership tree it would be an isolated manual API that the component model
  would likely replace. The limitation is documented instead.

## Revisit

When adding batching/scheduling, ownership/scopes, equality short-circuiting or
renderer-owned bindings. The push-invalidation/lazy-evaluation split and the
absence of computed disposal are the main triggers.
