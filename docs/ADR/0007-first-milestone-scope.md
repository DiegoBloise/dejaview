# ADR-0007: First milestone scope — first-mount rendering only

- **Status:** Accepted
- **Date:** 2026-11-11

## Context

The first milestone must prove the toolchain end to end:

```
Java → Maven → TeaVM → JavaScript → browser → DOM → interaction
```

It is tempting to start with signals, components and a diffing renderer.
Doing so would mix unvalidated compiler assumptions with unvalidated framework
design and make failures harder to diagnose.

## Decision

The first milestone implements only:

- an immutable view model (`View`, `ElementView`, `TextView`, `FragmentView`);
- attribute and event bindings;
- a first-mount renderer that appends a view tree to a container;
- a browser adapter and entry point;
- a counter example with a click handler that updates the DOM manually.

It deliberately does **not** implement signals, computed values, effects,
components, lifecycle hooks, diffing, hydration, routing or SSR.

The example uses `Dom.byId("count").setText(...)` inside its click handler. This
is a temporary, explicit escape hatch to prove event handling before the
reactivity engine exists. It is not the intended application API.

## Consequences

- The milestone has a small, reviewable surface.
- Compiler and browser failures are isolated from reactivity design.
- The example demonstrates manual DOM mutation rather than declarative reactive
  updates. This limitation is documented in `docs/IMPLEMENTATION_STATUS.md`.
- Phase 2 can replace the manual update with a tested reactivity core without
  rewriting the view model or renderer contracts.

## Alternatives considered

- **Implement signals first.** Rejected for this milestone: it would not prove
  the compiler/browser path faster and would expand the debugging surface.
- **Build a full component system first.** Rejected: too much unvalidated
  abstraction on top of an unvalidated toolchain.
- **Skip the example and test only on the JVM.** Rejected: the browser boundary
  is the core risk.

## Revisit

Immediately after the foundation milestone, when Phase 2 introduces the
reactivity engine and removes the manual DOM update from the example.
