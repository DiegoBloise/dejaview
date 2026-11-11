# ADR-0010: Batching, synchronous scheduling and reactive scopes

- **Status:** Accepted
- **Date:** 2026-11-11

## Context

Sprint 04 of the implementation roadmap requires batching, reactive scheduling,
execution ordering, protection against infinite update loops, disposal of
reactive scopes and error propagation. The first slices (ADR-0008 and ADR-0009)
deliberately left these out:

- every changed value ran its effects immediately, once per observer;
- there was no way to group writes into a single effect run;
- effects had to be disposed manually and computed values could not be disposed
  at all;
- mutually recursive effects could loop until the stack overflowed;
- a failing effect had no defined effect on other pending work.

These gaps make renderer integration and the component model harder, because
both need a predictable update boundary and a way to release resources with the
owning UI subtree.

## Decision

### Batching and scheduling

Add `Reactive.batch(Runnable)` backed by a package-private synchronous
scheduler:

- Effects are queued when invalidated and executed in first-in-first-out order.
- A queue entry is deduplicated, so an effect runs at most once per batch.
- Outside a batch the queue is flushed immediately, preserving the synchronous
  behavior of the existing API.
- Batches may be nested. Only the outermost batch flushes.
- A single flush is limited to 10,000 effect runs. Exceeding the limit throws
  `IllegalStateException` instead of looping forever.
- If an effect fails during a flush, the effect clears its dependencies, the
  remaining queue is discarded and the failure is rethrown. The scheduler is
  left usable for later updates.
- If a batch body fails, pending effects are still flushed and the original
  failure is rethrown; a flush failure is attached as a suppressed exception.

This is a synchronous scheduler, not a microtask or time-based scheduler. It
defines an execution boundary without changing the single-threaded model.

### Effect cleanup

Add `Reactive.onCleanup(Runnable)`:

- It may only be called inside an effect body.
- The callback runs before the next execution and when the effect is disposed.
- Multiple callbacks run in registration order; failures are collected and the
  first one is rethrown with the others suppressed.

This is separate from dependency cleanup, which remains automatic on every run
and on disposal.

### Reactive scopes

Add `ReactiveScope`:

- `scope.effect(...)`, `scope.computed(...)`, `scope.scope()` and
  `scope.add(AutoCloseable)` register owned resources.
- Disposal is idempotent and runs resources in reverse creation order.
- A failure while closing one resource does not prevent the others from being
  disposed; the first failure is rethrown with later failures suppressed.
- Creating a resource in a disposed scope throws `IllegalStateException`.
- Scopes nest: a child scope is just another owned resource.

Ownership is explicit. There is no implicit owner tree or automatic scope
attached to a component yet; that belongs to the component model.

### Computed disposal

`Computed<T>` now implements `AutoCloseable` and gains `dispose()` /
`isDisposed()`:

- Disposal clears dependency registrations and releases the cached value.
- `get()` after disposal throws `IllegalStateException`.
- Disposing while the computed is evaluating throws.

This revisits the ADR-0009 decision to leave computed values without a disposal
lifecycle. The reason for the earlier deferral was the absence of an ownership
model; `ReactiveScope` now supplies one.

## Consequences

- Multiple writes can be grouped into a single, deduplicated effect run.
- Effect order within a flush is deterministic (invalidation order).
- Runaway mutual updates fail fast with a clear error instead of hanging.
- Effects can release resources they create, both before re-execution and on
  disposal.
- Reactive resources can be released as a group, preparing the component model.
- Execution is still synchronous and single-threaded. There is no microtask
  scheduler, time slicing or cross-thread support.
- There is no explicit topological ordering of the dependency graph. FIFO
  scheduling plus lazy computeds is sufficient for the current feature set;
  glitch-free topological propagation remains future work.
- Reentrant invalidation still fails fast. In particular, an effect that writes
  to a signal which invalidates a computed it depends on is rejected.
- `Computed` disposal does not notify its observers. Reading a disposed computed
  fails on the next read instead.

## Alternatives considered

- **Microtask-based scheduler.** Rejected for this sprint: it changes observable
  timing, needs a platform hook and would complicate JVM tests before the
  renderer needs it.
- **Implicit batching on every signal write.** Rejected: explicit
  `Reactive.batch` keeps the boundary visible and avoids changing existing
  one-write behavior silently.
- **Topological execution ordering.** Deferred: the current primitives are lazy
  and FIFO ordering is predictable. A topological scheduler should be designed
  together with glitch-free computed propagation.
- **Automatic owner tree tied to components.** Deferred to the component model,
  which does not exist yet. `ReactiveScope` is the minimal explicit primitive.
- **Keep `Computed` non-disposable.** Rejected now that scopes exist; leaving it
  non-disposable would make scope ownership incomplete.

## Revisit

When the component model introduces automatic ownership, when renderer bindings
need a scheduled update pipeline, or when glitch-free/topological propagation is
required. The 10,000-step loop guard and the synchronous flush boundary are the
main parameters to revisit.
