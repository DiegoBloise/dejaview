# ADR-0021: CLI and development server model

- **Status:** Accepted
- **Date:** 2026-11-13

## Context

Sprint 14 (Phase V) had to deliver the official developer workflow for DejaView
applications: project creation, build, and a development server with live
reload. Constraints and preconditions:

1. DejaView applications already build with the Maven wrapper (`./mvnw`); the
   CLI is an orchestration layer over Maven, not a second build system.
2. The CLI runs before — and independently of — any project build, so it must
   have **zero runtime dependencies**. `com.sun.net.httpserver.HttpServer` in
   the JDK is sufficient for the development server; no servlet container, no
   daemon, no background service.
3. Until DejaView is published on Central, generated projects resolve
   `io.dejaview:*` from the local Maven repository (`./mvnw install` once).
4. No reflection, no global singletons — the CLI is plain Java 25 objects.
5. Failures must be actionable: Maven output is reduced to the `[ERROR]`
   lines (boilerplate filtered, bounded) and the development server keeps
   serving the last good build when a rebuild fails.
6. The development server is a development tool, not a production server: no
   caching, no compression, no TLS. Production deployment stays "upload the
   static files" (Sprint 15+ scope).

## Decision

### `dejaview-cli`: a zero-dependency orchestration module

One Maven module (`io.dejaview:dejaview-cli`), jar with manifest
`Main-Class: io.dejaview.cli.Main`, no dependencies. Commands:

- `dejaview new <name> [--package p] [--group-id g] [--directory dir]`
- `dejaview build [-- <maven args>]`
- `dejaview run [--port N] [--host H] [--no-build]`
- `dejaview doctor [--no-port-check]`
- `dejaview help`, `dejaview --version`

`CliException` is the only user-facing failure channel: its message is printed
as-is, stack traces are never shown. Exit codes: `0` success, `1` user-facing
failure (bad layout, failed build, unreadable config), `2` usage errors
(unknown command, unknown option). `main` delegates to a testable
`execute(String[], Path)` returning the code.

### `dejaview new`: an embedded, self-contained template

The `basic-app` template lives in `src/main/resources/templates/basic-app/`.
Because classpath directories cannot be enumerated from a jar, the template's
files are declared in `files.txt`. Placeholders (`__NAME__`, `__ARTIFACT_ID__`,
`__GROUP_ID__`, `__PACKAGE__`, `__DEJAVIEW_VERSION__`, `__TEAVM_VERSION__`) are
replaced byte-wise (all ASCII, safe for UTF-8); the package path placeholder is
also substituted in file *paths* (`__PACKAGE_PATH__`).

- `gitignore` is shipped without the leading dot — Maven's default resource
  excludes strip `**/.gitignore` from jars — and restored to `.gitignore` on
  extraction.
- `mvnw` is extracted with the `rwxr-xr-x` POSIX permissions; the Windows shell
  fallback is `mvnw.cmd`.
- The target directory must be absent or empty; the generator never overwrites.
- The generated `pom.xml` is deliberately **not** a child of `dejaview-parent`:
  a generated project is standalone, with explicit plugin versions, the same
  TeaVM layout as `examples/counter` (copy-webapp → compile → enforcer artifact
  check), and `dejaview.version` pinned to the CLI's own version
  (`Version.current()`, manifest-driven with a `0.1.0-SNAPSHOT` fallback).
- The wrapper is the only-script wrapper (`wrapperVersion=3.3.4`), embedded
  verbatim from the repository — one wrapper version, one source of truth.

### `dejaview.properties`: per-project configuration

Keys: `port` (default 4321), `host` (default `127.0.0.1`), `webappDir` (default
`target/webapp`). Comments (`#`, `!`) and blank lines are allowed; unknown
keys, missing `=`, non-numeric or out-of-range ports fail with the file and
line number. Command-line flags take precedence over the file. A missing file
yields defaults; this is the normal case.

### Development server: JDK HTTP server, injected live reload

`com.sun.net.httpserver.HttpServer` serves `webappDir`:

- Paths are normalized and must stay under the web root (`403` otherwise);
  directories serve `index.html`; `404` with a plain message otherwise.
- Correct content types (`application/javascript`, `text/html`, `image/*`,
  JSON, fonts), `Cache-Control: no-store` everywhere, `X-Content-Type-Options:
  nosniff`, GET/HEAD only.
- HTML responses get an injected `<script src="/__dejaview__/live.js" defer>`
  before `</body>` (or appended) unless already present.
- `/__dejaview__/live.js` is a tiny poll client: every second it fetches
  `/__dejaview__/version` and reloads the page when the value changes.
- `/__dejaview__/version` returns a monotonic counter — the **only** state
  the server holds. `bumpVersion()` is called by the watcher after a
  successful rebuild; the next poll observes it.

### Watch loop: WatchService with debounce, inline rebuilds

`BuildWatcher` registers `src/main/java`, `src/main/webapp`, `src/test/java`
recursively (new directories registered on `ENTRY_CREATE`) on a `WatchService`,
on a virtual thread, with a 300 ms debounce. Rebuilds run **inline** on the
watcher thread, so two builds never overlap; events that arrive during a
build are observed afterwards and trigger one further rebuild. A failed
rebuild prints diagnostics and leaves the last good build serving.

### Diagnostics and doctor

Build failures print the `[ERROR]` lines (up to 20), with Maven's boilerplate
headers (`BUILD FAILURE`, `-> [Help 1]`, "Re-run Maven…") filtered out, plus
pointers to `dejaview doctor`. When no `[ERROR]` lines exist, the last 25
lines are printed instead. `doctor` checks: JDK ≥ 25, system Java compiler,
wrapper presence/executability, `pom.xml`, `src/main/java`,
`src/main/webapp/index.html`, `dejaview.properties` parseability, local
`io.dejaview` availability (WARN), and port availability (WARN,
`--no-port-check` to skip). Any FAIL exits 1.

### Testing

- **JVM unit tests (28)**: template generation and placeholder substitution,
  `dejaview.properties` parsing, diagnostics extraction/filtering/capping,
  dev-server static serving, traversal guard, content types, live-reload
  injection, version endpoint, CLI dispatch and `doctor` in empty/generated
  projects. Ephemeral ports everywhere; never 4280.
- **CliWorkflowIT** (opt-in `e2e` profile): installs the root modules when
  missing, generates a project, builds it with its generated wrapper, serves it,
  runs it in Chromium (asserts the counter renders and increments), edits a
  source file, and asserts the watcher rebuilds, the version bumps and the
  page reloads itself. 5/5 pass.

## Consequences

- The developer workflow is two commands: `dejaview new my-app && dejaview run`
  (after one `./mvnw install -DskipTests` until Central publishing).
- `./mvnw` stays the single build entry point; the CLI adds nothing to the
  build semantics — it orchestrates and decorates.
- Generated projects are self-contained (own wrapper, own pom) and pinned to
  the CLI's DejaView version — no parent POM coupling.
- The dev server is intentionally not production-grade: no caching, TLS or
  compression; production deployment is static files on any host.
- `gitignore` special-case in the generator is contained and documented; if
  Maven ever preserves dotfiles, the rename can be dropped.

## Alternatives considered

- **A Maven plugin (`mvn dejaview:run`).** Rejected for now: forces users into
  Maven lifecycle introspection before tooling can run; the CLI orchestrating
  the wrapper is simpler and works from an empty directory. A plugin goal may
  be layered on top later without changing the CLI.
- **Gradle or npm-based toolchain.** Rejected: violates the Maven Wrapper-only
  constraint (AGENTS.md); a second build tool doubles maintenance for zero
  user value.
- **WebSocket live reload.** Rejected for now: the poll-based version check
  needs no dependencies and no protocol; a WebSocket would reintroduce a JS
  client dependency. Revisit if sub-second reload latency becomes a goal.
- **Content-hash file watching.** Rejected: mtime events plus the 300 ms
  debounce are sufficient; hashing adds I/O with no user-visible gain here.
- **Embedding the full TeaVM toolchain in the CLI.** Rejected: duplicates the
  build; the generated pom wires TeaVM the same way as the examples.

## Revisit

When DejaView publishes to Central, the generated pom should reference the
released version, and `doctor`'s local-repository check should verify Central
reachability instead. If a native `dejaview` launcher ships (jpackage or
installer script), only the invocation guidance in the docs changes — the
CLI's surface stays. If the dev server needs HTTPS or proxying, that is a
separate ADR; it does not belong in a development tool by default.
