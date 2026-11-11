# ADR-0002: JavaScript is the only browser target

- **Status:** Accepted (project constraint)
- **Date:** 2026-11-11

## Context

DejaView needs a browser execution strategy. Several JVM-to-browser approaches
exist, including WebAssembly-based runtimes and JavaScript transpilation. The
project has an explicit, non-negotiable requirement: JavaScript is the only
browser compilation target.

## Decision

JavaScript is the only browser target.

- TeaVM is configured with `targetType=JAVASCRIPT`.
- WebAssembly, WASM GC, WASI and alternative browser execution environments are
  out of scope.
- No module may introduce a WebAssembly build path.

## Consequences

- The framework integrates directly with the browser's JavaScript and Web APIs.
- Output is constrained by TeaVM's JavaScript backend and the target browser's
  JavaScript support.
- Performance work focuses on generated JavaScript quality and runtime design,
  not on a WASM backend.
- TeaVM features that only apply to WASM targets are not used.

## Alternatives considered

None. This is an approved project constraint, not an open design choice.

## Revisit

Only with explicit project authorization. Any change would require a new ADR
that supersedes this one.
