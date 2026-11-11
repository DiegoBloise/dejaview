# ADR-0003: Initial module boundaries

- **Status:** Accepted
- **Date:** 2026-11-11

## Context

The long-term architecture lists many modules. Creating all of them before
there is code to justify them would produce empty artifacts, fake boundaries and
maintenance overhead. The first milestone needs only enough structure to prove
the stack and establish dependency rules.

## Decision

The initial build contains three modules:

```text
dejaview-core      platform-independent view model
dejaview-dom       DOM contracts, renderer and browser adapter
examples/counter   end-to-end example and browser E2E test
```

Dependency direction:

```text
examples/counter → dejaview-dom → dejaview-core
```

Rules:

1. `dejaview-core` must not import browser APIs, TeaVM types or DOM types.
2. `dejaview-dom` owns the browser adapter and the only TeaVM JSO imports in
   the framework.
3. The renderer lives in `dejaview-dom` for now because it targets DOM-shaped
   contracts. A separate renderer module will be introduced when a second
   rendering backend (for example SSR) needs to share it.
4. New modules are added only when a milestone needs them and when their
   responsibility cannot be placed coherently in an existing module.

## Consequences

- The module graph is small and has no cycles.
- The platform-neutral core is testable on the JVM.
- Some future modules will need to be extracted from `dejaview-dom` or created
  from scratch. That is deliberate; premature extraction would be harder to
  validate.
- The README's full module table is a roadmap, not a description of the current
  build.

## Alternatives considered

- **Create the full module tree now.** Rejected: empty modules do not validate
  boundaries and slow the build.
- **Single module.** Rejected: it would make it easy to leak browser APIs into
  the core and would not validate the architecture's central separation.
- **Separate renderer module immediately.** Rejected: the renderer currently
  depends on DOM-shaped contracts and has one backend. Extract it when the
  second backend exists.

## Revisit

When SSR, hydration or a second rendering backend is implemented.
