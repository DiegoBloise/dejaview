# ADR-0008: Synchronous signals and effects for the first reactivity slice

- **Status:** Accepted
- **Date:** 2026-11-11

## Context

Phase 2 introduces fine-grained reactivity. The first milestone deliberately left
the counter example updating the DOM by hand, with `Dom.byId("count").setText(...)`
inside the click handler (ADR-0007).

The full target engine includes computed values, batching, scheduling, ownership
scopes, circular-dependency detection and renderer-owned bindings. Implementing
all of that at once would make the first reactive slice difficult to review and
would mix API design with renderer integration.

The view model is immutable and the DOM layer is platform-specific. Reactivity
must be usable without either of them so it can be tested on the JVM and reused
by future renderers.

## Decision

Create a new platform-independent module, `dejaview-reactivity`, and implement
only two primitives in this slice:

1. `Signal<T>` with `get`, `set` and `update`.
2. `Effect` with automatic dependency tracking, dependency re-collection and
   explicit `dispose()` / `close()`.

Semantics:

- Reading a signal inside an effect registers a dependency.
- Writing a different value notifies observers synchronously.
- Writing an equal value is a no-op (`Objects.equals`); no observer runs.
- `null` is not a valid signal value.
- Dependencies are re-collected on every effect run, so conditional reads work.
- Effects run immediately on construction.
- Effects are not thread-safe. They target the single-threaded browser runtime
  and single-threaded JVM tests.
- An effect cannot write to a signal it depends on, and an effect invalidated
  while it is running throws `IllegalStateException`. This prevents silent
  infinite loops until batching and cycle handling exist.
- A failed effect run clears its dependencies before rethrowing, so it does not
  remain registered with signals it read before the failure.

The counter example now stores its count in a `Signal<Integer>`. An `Effect`
reads the signal and updates the text node; the click handler only calls
`count.update(...)`. The effect still resolves the node with `Dom.byId("count")`.
That direct lookup is a bridge until the renderer owns reactive bindings.

## Consequences

- `dejaview-reactivity` has no dependency on `dejaview-core`, `dejaview-dom` or
  TeaVM and is covered by JVM unit tests.
- The example demonstrates a real reactive update through the whole stack:
  signal change → effect rerun → DOM text change, validated in Chromium.
- The public API is small and explicit (`new Signal<>(0)`, `new Effect(...)`).
- There is no `Computed`, batching, scheduler or ownership tree yet. Effects run
  once per changed value and must be disposed manually.
- Renderer integration is not done. Application code still performs a DOM lookup
  inside the effect, which is not the intended long-term API.
- Reentrant/cyclic updates fail fast instead of being scheduled or resolved.

## Alternatives considered

- **Implement the complete reactivity engine first.** Rejected: computed values,
  batching, scheduling and ownership would expand the review surface and delay
  end-to-end validation of the first reactive update.
- **Put signals in `dejaview-core`.** Rejected: reactivity is a distinct
  responsibility, must remain usable without the view model, and the target
  architecture already reserves a module for it. ADR-0003 allows new modules
  when a milestone needs them.
- **Use manual subscription (`Signal.subscribe`) in the example.** Rejected: it
  would introduce a callback API that the intended effect-based model does not
  need.
- **Make effects asynchronous immediately.** Rejected: batching and scheduling
  need a scheduler design and tests; synchronous execution is the smallest
  correct behavior for this slice.

## Revisit

When adding `Computed`, batching/scheduling, effect ownership, renderer-owned
bindings or cycle detection. The fail-fast reentrancy rules should be replaced by
the scheduler semantics at that point.
