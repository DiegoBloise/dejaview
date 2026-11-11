# ADR-0004: DOM interoperability strategy

- **Status:** Accepted
- **Date:** 2026-11-11

## Context

The renderer must create elements, set attributes, append nodes and register
event listeners in the browser. It must also be testable without a browser.
Directly calling TeaVM JSO APIs throughout the renderer would make the logic
JVM-untestable and couple the framework to TeaVM.

## Decision

Introduce a small, platform-neutral DOM contract in `dejaview-dom`:

```java
DomNode
DomElement   setAttribute, setText, append, addEventListener
DomDocument  createElement, createText, getElementById
```

The renderer depends only on these interfaces. A browser adapter
(`BrowserDomDocument`, `BrowserDomElement`, `BrowserDomNode`) implements them
using TeaVM JSO. A fake implementation in the test sources implements the same
contracts for JVM tests.

The interfaces are intentionally narrow. They are not an attempt to model the
entire DOM; they expose only what the current renderer needs.

## Consequences

- Renderer traversal, ordering, attributes, fragments and event registration are
  covered by fast JVM tests.
- JSO imports are confined to three adapter classes.
- The browser boundary is validated by a Playwright E2E test.
- Adding a renderer capability may require extending the contracts and both
  implementations.
- `DomElement.append` rejects nodes from a different implementation, which
  surfaces accidental mixing early.

## Alternatives considered

- **Call JSO directly from the renderer.** Rejected: untestable on the JVM and
  tightly coupled to TeaVM.
- **Use Elemental2.** Rejected for the initial milestone: it adds a large
  GWT-oriented API surface and another dependency, while the required DOM
  operations are small.
- **Build HTML strings and assign `innerHTML`.** Rejected: it complicates
  escaping and XSS safety, bypasses native node APIs and makes event wiring
  harder.

## Revisit

When the renderer grows beyond the current narrow contract, or when a second
platform (SSR) needs the same interfaces.
