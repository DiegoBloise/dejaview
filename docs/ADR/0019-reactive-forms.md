# ADR-0019: Reactive forms model

- **Status:** Accepted
- **Date:** 2026-11-13

## Context

Sprint 12 had to deliver `dejaview-forms`: reactive form state, validation and
DOM bindings built on `dejaview-core` and `dejaview-reactivity`, so developers
can create and validate forms using plain Java APIs with no manual listener
wiring.

Constraints and preconditions:

1. The module must stay platform-neutral: `dejaview-core` and
   `dejaview-reactivity` must not import TeaVM or browser types. Browser DOM
   specifics live in `dejaview-dom` behind the `DomElement` contract.
2. The browser is single-threaded; reactive updates are synchronous outside an
   explicit `Reactive.batch`. Validation runs on value/validator changes, not on
   a microtask scheduler.
3. Form values are never `null`: the type's empty value (`""`, `0`, `false`)
   represents "not filled in", matching `Signal`'s non-null contract.
4. Validation error keys are stable codes (`required`, `email`, `minLength`,
   `taken`, …). The framework is language-neutral: the application maps keys to
   localized copy.
5. HTML semantics matter: disabled children are excluded from group/array
   values (like disabled inputs in a submitted form), and group/array errors are
   prefixed by name/index so the caller knows exactly which child failed.

## Decision

### Control model: `AbstractControl` → `FormControl` / `FormGroup` / `FormArray`

`AbstractControl<V>` owns a value, validation state and the `touched`/`dirty`
/`disabled` flags. Reading any of them inside an effect or render function
tracks it reactively; writing notifies observers synchronously. Each control
starts one internal validation effect through `initialize()`, called at the end
of subclass constructors.

- `FormControl<V>` holds one value. `setValue` marks it dirty and revalidates.
- `FormGroup` holds named children (`Map<String, AbstractControl<?>>`). Its
  value is an immutable map excluding disabled children; its errors prefix child
  errors with the child name (`email.required`). It aggregates the status,
  touched and dirty flags of its children.
- `FormArray` holds ordered children (`List<AbstractControl<?>>`). Its value is
  an immutable list excluding disabled children; errors prefix with the index
  (`tags.0.required`). It supports `add`, `insert` and `removeAt`.

Composite `value()`, `errors()` and `status()` are computed on demand from
child signals — no effect writes its own dependencies.

### Validation: synchronous first, asynchronous only when sync passes

Each control holds `Validator<V>` and `AsyncValidator<V>` lists. On every
validation run:

1. Synchronous validators run first and merge their errors.
2. If any sync error exists, asynchronous validators are skipped entirely — no
   request is fired for a value that cannot succeed.
3. Otherwise every async validator runs; results merge through an
   `AsyncAccumulator` keyed by a generation counter. A callback from a
   superseded run (stale value) is ignored, so an outdated response cannot
   overwrite newer errors nor leave a control permanently pending.

`ControlStatus` is `VALID`, `INVALID`, `PENDING` or `DISABLED`. Disabled
controls report `DISABLED` regardless of their errors; a disabled ancestor
disables all descendants effectively.

### Lifecycle flags

`reset()` restores initial values and clears touched/dirty but does not
re-enable disabled controls. `dispose()` (idempotent) increments the async
generation, clears `pending`, disposes descendants and closes the internal
reactive scope, so late async results cannot touch a disposed control.

### Built-in validators report stable keys

`Validators` provides `required`, `minLength`, `maxLength`, `email`, `pattern`,
`min`, `max` and `custom`. Each returns a stable key and no message; string
validators ignore empty values so "empty" and "malformed" remain separate
decisions (only `required` flags a blank value). Applications map keys to
messages — the showcase maps `required` → `"This field is required."`,
`taken` → `"That username is already taken."`, etc.

### DOM value events: `EventValueHandler` / `onValue` / `addValueEventListener`

The core module gains `EventBinding(type, valueHandler)` — an event binding
whose handler receives the target value and checked state — and
`ElementView.onValue(eventType, valueHandler)`. `dejaview-dom` adds
`DomElement.addValueEventListener(type, DomValueListener)`; `BrowserDomElement`
extracts `event.target.value`/`event.target.checked` through TeaVM JSO bridges
and coerces boolean property strings `"true"`/`"false"` to booleans when setting
properties. `RenderTreeBuilder` dispatches value-aware bindings to
`addValueEventListener` and plain bindings to `addEventListener`.

`FormBindings` builds `input`, `textarea`, `checkbox` and `select` elements
bound to a `FormControl`: value events update the control, blur marks it
touched, `aria-invalid` reflects the control status, and `disabled` reflects
its disabled state. Typed text lands on `input`; selects and checkboxes use
`change`.

### Submission snapshot

`AbstractControl.submit()` marks everything touched, revalidates and returns a
`FormSubmission<V>(valid, value, errors)` with an `ifValid(consumer)` guard.

## Consequences

- `dejaview-forms` is a new pure-Java module depending on `dejaview-core` +
  `dejaview-reactivity`; 53 JUnit tests cover control values, groups, arrays,
  validators, error prefixing, async validation, stale-result rejection,
  disposal, submission and DOM bindings.
- Core/DOM value-event plumbing is tested (`ElementViewTest`, `DomRendererTest`)
  and used by `FormBindings`.
- The showcase ships `FormsDemo` (`?example=forms`) with sync + async
  validation, a dynamic tag array and a submission snapshot, verified by
  `ShowcaseBrowserIT#formsExampleValidatesAndSubmits` in Chromium.
- The website gains a bilingual "Reactive forms" feature card and tutorial
  section (tutorial headings renumbered to 14–18 in both languages), a forms
  playground example and `WebsiteBrowserIT` coverage (feature cards 9→10,
  examples 8→9).
- The `touched`/`dirty` separation means validation messages should key off
  `touched`, not `dirty` — an intentional UX choice documented in the tutorial.
- Disabled children are excluded from values but their controls remain usable;
  `reset()` never re-enables a disabled control — applications must call
  `enable()` explicitly.

## Alternatives considered

- **A dedicated `Form` type separate from `FormGroup`.** Rejected: `FormGroup`
  already aggregates children, exposes `submit()` and derives `value`; a thin
  `Form` alias adds no semantics and complicates the API surface.
- **Effect-per-validator.** Rejected: one validation effect per control keeps
  dependency tracking simple and avoids an effect writing its own dependencies.
- **Async validators always run, even when sync fails.** Rejected: running a
  slow network check for a value that already fails required validation wastes
  the round-trip and confuses users; skipping when sync fails is the standard
  behavior.
- **Promise-based async validation.** Rejected: TeaVM 0.16.0's typed bindings
  cover XHR and timers; `JSPromise` blocking `await()` is unusable on the
  browser event loop. A completion-callback contract (`ValidationCallback`) is
  the whole contract, with generation counters to discard stale responses.
- **`null` as "empty" for optional controls.** Rejected: `Signal` disallows
  null; using the type's empty value keeps every signal non-null and every
  value() call type-safe.

## Revisit

When fine-grained binding nodes land (currently not planned), `FormBindings`
could update individual DOM attributes instead of rebuilding the view on every
render pass. If framework-level localized messages become desirable, a separate
message-bundle layer can map stable error keys to copy without changing the
validator contract.
