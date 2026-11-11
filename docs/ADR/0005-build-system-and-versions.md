# ADR-0005: Build system, versions and metadata

- **Status:** Accepted
- **Date:** 2026-11-11

## Context

The project needs a reproducible multi-module build with centralized versions,
consistent compiler settings and a wrapper so contributors do not depend on a
globally installed Maven.

## Decision

- **Build system:** Apache Maven, multi-module.
- **Wrapper:** Maven Wrapper 3.3.4, Maven distribution 3.9.16.
- **Parent POM:** `io.dejaview:dejaview-parent:0.1.0-SNAPSHOT`, packaging `pom`.
- **Java:** JDK 25 required; `maven.compiler.release=25`.
- **Compiler plugin:** `maven-compiler-plugin` 3.16.0.
- **Tests:** JUnit Jupiter via `junit-bom` 5.14.4, Surefire/Failsafe 3.6.1.
- **Browser compiler:** TeaVM 0.16.0.
- **E2E:** Playwright Java 1.64.0, opt-in through the `e2e` profile.
- **Enforcement:** `maven-enforcer-plugin` requires JDK 25+, Maven 3.9+ and no
  duplicate dependency versions.

All dependency and plugin versions are declared in the parent POM. Modules
declare only their direct dependencies and module-specific configuration.

Group id `io.dejaview` and version `0.1.0-SNAPSHOT` are the initial coordinates.
They are not a public API stability promise.

## Consequences

- A clean checkout builds with `./mvnw clean verify`.
- Version upgrades happen in one place.
- The build fails early on an unsupported JDK or Maven version.
- `mvnw` must remain executable in version control.
- Gradle is not introduced. No Gradle files are part of the project.

## Alternatives considered

- **Gradle.** Rejected by project decision; Maven is the exclusive build system.
- **Per-module versions without a parent.** Rejected: drift and duplicated
  configuration.
- **No wrapper.** Rejected: environment-dependent builds.

## Revisit

When the project publishes its first release, the versioning and release
strategy will need a dedicated ADR (artifact signing, publishing, changelog and
API stability).
