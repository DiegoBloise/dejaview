# ADR-0022: Production compiler pipeline and `dejaview-compiler`

- **Status:** Accepted
- **Date:** 2026-11-13

## Context

Sprint 15 (Phase V) had to turn "the counter example compiles and runs" into a
reliable production compilation pipeline for DejaView applications, with the
acceptance criterion: a DejaView application must be compilable into
production-ready JavaScript using a reproducible Maven workflow.

Constraints and preconditions:

1. ADR-0001/ADR-0005 reserve compiler replaceability behind the build
   boundary; a second toolchain (Bazel, Gradle) is out of the question.
2. Browser-targeted modules share one TeaVM configuration, but the configuration
   was copy-pasted in each module and had already drifted in subtle ways.
3. The build must fail loudly and actionably when the output is broken — a
   silently empty or stale bundle is the worst failure mode for a static site.
4. No reflection, no second build system, no runtime dependency for any of
   this — build-time tooling only.
5. DejaView applications deploy as static files; "production" therefore means
   an optimized, validated, fingerprinted static bundle — not a server.

## Decision

### The compiler abstraction is the Maven build boundary, not a Java wrapper

`dejaview-compiler` is a build-time-only module (zero runtime dependencies,
no browser API, no TeaVM dependency). TeaVM remains the compiler (ADR-0001);
the module formalizes everything *around* it: profiles, validation, asset
fingerprinting, diagnostics and reproducibility verification. An in-process
wrapper around the TeaVM API was rejected: it would duplicate the plugin's
orchestration (class path handling, incremental builds, JDK detection) with no
additional reach, and the plugin boundary is already the replaceability seam
ADR-0001 needs.

### One TeaVM configuration, declared once

The `teavm-maven-plugin` configuration lives in the parent POM's
`pluginManagement`; browser modules only declare `<mainClass>`. Centralized:
`targetType=JAVASCRIPT`, `targetDirectory=${project.build.directory}/webapp`,
`targetFileName=app.js`, `jsModuleType=NONE`, `strict=true`,
`sourceMapsGenerated`, `optimizationLevel`, and the output-name minification
switch. All are parameterized by `dejaview.compiler.*` properties.
`maven-resources-plugin`'s `copy-webapp` execution is likewise centralized.

### Two build profiles

| Profile | optimization | minified | size tripwire | Purpose |
|---|---|---|---|---|
| `development` (default) | `SIMPLE` | `false` (readable output) | 2 MiB | debugging, live reload |
| `production` (`-Pproduction`) | `FULL` (TeaVM dead-code elimination) | `true` | 768 KiB | release artifacts |

`CompilerProfile` in `dejaview-compiler` mirrors the same mapping for the
build tooling; the POM properties remain the source of truth for the plugin.

### Verified against the TeaVM plugin descriptors during this sprint

- TeaVM's optimization enum is `SIMPLE`/`ADVANCED`/`FULL`; `FULL` applies the
  most aggressive DCE and was chosen for production (bundle smoke-tested in a
  real browser for the whole browser suite: Counter, Showcase and Website
  browser tests all pass on `-Pproduction` builds).
- The plugin's output-name switch is **`minifying`** (`${teavm.minifying}`,
  default `true`); the previously used `<minified>` element is not a parameter
  of the JS compile mojo and was silently ignored, so every development build
  shipped obfuscated JavaScript. The parent POM and the generated template now
  bind `minifying` to `dejaview.compiler.minify`. Effect: development builds
  are readable again (larger, unminified bundles); production builds are
  minified as intended.

### Validation and fingerprinting on every build

`dejaview-compiler` provides, all dependency-free:

- `WebAppValidator` / `ValidatorOptions` — errors (fail the build): the
  webapp directory is missing, `index.html` is missing (unless `--no-index`),
  a local `src`/`href` reference escapes the web root or points at a missing
  file, `app.js` is missing or empty. Warnings (reported, tolerated): a
  missing source map, a webapp larger than the size budget, root-relative and
  `file:///` references.
- `AssetManifest` — deterministic `dejaview-assets.json` next to the bundle:
  every file with relative path, byte size and SHA-256, sorted by path; the
  manifest excludes itself; equivalent builds produce byte-identical manifests.
- `VerifyWebApp` (`verify` phase for the in-repo examples, bound via
  exec-maven-plugin; `package` phase for the generated template, because the
  DejaView CLI builds generated projects with `package`) — validates, then
  writes the manifest; on errors it throws `IllegalStateException` carrying
  the human-readable report, which fails the build. The website example
  passes `--no-index` because its host pages live in `website/`, not in its
  own webapp directory.
- `Hashing` — JDK-only SHA-256 helpers shared by the manifest and the
  reproducibility checks.

Build diagnostics ship in `dejaview-cli` (`io.dejaview.cli.BuildDiagnostics`,
zero dependencies): it extracts the actionable `[ERROR]` lines (or a bounded
tail when none are present) and maps common TeaVM/Maven failure signatures
(class file version, missing `teavm-classlib`, unresolved classes, strict
mode) to likely-cause hints, surfaced by `dejaview build` and by the
development server's rebuild-failure path. The zero-dependency constraint for
the CLI (ADR-0021) wins over a shared build-time diagnostics library.

### Reproducible output

`project.build.outputTimestamp=2026-11-13T00:00:00Z` pins archive timestamps.
TeaVM's JavaScript output is deterministic in practice: `ReproducibleBuildIT`
(opt-in `e2e` profile) builds the counter example twice and asserts
byte-identical `app.js` and manifest. Equivalent build conditions
(same inputs, same environment) are the reproducibility contract, not
cross-machine identity.

### Testing

- JVM unit tests (31): validator, manifest, hashing, profiles, `VerifyWebApp`
  CLI. Build diagnostics are covered in `dejaview-cli` (`BuildDiagnosticsTest`).
- Browser e2e under `-Pproduction` for the whole browser suite (Counter 1/1,
  Showcase 11/11, Website 4/4) — runtime behavior of optimized bundles
  matches development builds.
- `ReproducibleBuildIT` (dejaview-compiler `e2e` profile): two nested builds,
  byte-comparison of bundle and manifest.
- `CliWorkflowIT` `@Order(6)`: the generated application writes the manifest
  during `package`.

## Consequences

- `./mvnw clean package -Pproduction` is the release build; `./mvnw clean verify`
  (development) remains the everyday gate and now also validates the webapp.
- Generated projects (`dejaview new`) validate on every `package` and fingerprint
  their output — production readiness is the default path, not a plugin opt-in.
- The website documents the pipeline; the CLI's `build`/`run` unchanged in
  interface.
- Development bundles are larger (unminified); the size budget defaults
  account for that (2 MiB development / 768 KiB production) and are a warning,
  never a failure.

## Alternatives considered

- **A dedicated Maven plugin** (`mvn dejaview:build`). Deferred: it cannot be
  published until DejaView is on Central, and exec-maven-plugin plus the
  generated `dejaview-compiler` tooling covers the same ground today with
  plain, inspectable configuration. Can be layered later without changes to
  the pipeline.
- **In-process wrapper around the TeaVM API.** Rejected (see above): duplicates
  the plugin's orchestration; the Maven boundary is the seam ADR-0001 chose.
- **Terser or another JS minifier stage.** Rejected for now: TeaVM's
  `minifying` output is sufficient and keeps the pipeline dependency-free;
  revisit if real-world bundle sizes demand more compression.
- **`verify`-phase-only validation.** Rejected for the generated template:
  the DejaView CLI builds with `package`, so validation bound to `verify`
  would never run in the developer workflow; the template therefore binds
  `verify-webapp` to `package` (declaration order after the TeaVM execution).
  The in-repo examples keep `verify` (the repository gate is `mvn verify`).
- **Failing on budget/source-map warnings.** Rejected: they are diagnostics;
  only broken artifacts fail the build.

## Revisit

When DejaView publishes to Central, generated projects should reference the
released `dejaview-compiler` version directly. If TeaVM renames `minifying`
or changes `optimizationLevel` semantics, the parent POM and this ADR (not the
module code) are the places that change. If a JS compression stage becomes
necessary, add it after validation, not before it.
