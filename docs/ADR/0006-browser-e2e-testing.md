# ADR-0006: Browser end-to-end testing with Playwright Java

- **Status:** Accepted
- **Date:** 2026-11-11

## Context

Unit tests with a fake DOM prove renderer logic but not the actual browser
boundary: JSO interop, TeaVM output, script loading and real event dispatch.
The project needs an automated browser check without adding a second build
system or a Node.js toolchain.

## Decision

Use **Playwright Java** in an opt-in Maven profile:

```bash
./mvnw -Pe2e -pl examples/counter -am verify
```

- The `e2e` profile adds Playwright as a test dependency.
- `exec-maven-plugin` runs Playwright's CLI to install Chromium during
  `pre-integration-test`.
- `maven-failsafe-plugin` runs `*IT.java` tests during `integration-test`.
- The test starts a JDK `HttpServer` serving `target/webapp`, launches headless
  Chromium, asserts the rendered heading and counter, clicks the button twice
  and asserts the updated text.
- Page errors are collected and fail the test.

The default build does not compile or run E2E sources; Playwright is not needed
for ordinary development or CI unit tests.

## Consequences

- The browser boundary is validated automatically.
- Maven remains the only build system; no `package.json` is required.
- The first E2E run needs network access to download Chromium and may need
  system libraries in minimal CI images.
- Playwright's driver writes to a native stream in the forked test JVM, which
  produces a harmless "Corrupted channel" warning in some environments.
- The E2E profile is slower than the default build and is therefore separate.

## Alternatives considered

- **Node.js Playwright.** Rejected: it would introduce a second package manager
  and build ecosystem, conflicting with the Maven-only build decision.
- **Selenium.** Viable, but browser/driver management is heavier and Playwright
  provides a cleaner API for the assertions needed here.
- **Manual browser testing only.** Rejected: not reproducible and not
  automated.
- **HtmlUnit or a JS-less DOM.** Rejected: it would not validate the real
  browser/JSO boundary.

## Revisit

When the project needs a cross-browser matrix (Firefox/WebKit), visual
regression testing or CI browser caching.
