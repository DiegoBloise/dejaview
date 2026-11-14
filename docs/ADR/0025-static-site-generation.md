# ADR-0025: Static site generation and `dejaview-ssg`

- **Status:** Accepted
- **Date:** 2026-11-14

## Context

Phase VI (Sprint 18) turns the server-side rendering pipeline of Sprint 17
(ADR-0024) into a build-time capability: an application must be able to emit a
deployable static site — a directory of complete HTML pages, copied assets and
a sitemap — produced without a server runtime or a browser. Later sprints
build on it: hydration (Sprint 19) and a Spring Boot adapter (Sprint 21)
consume the same `PageMeta` and pipeline contracts.

Constraints and preconditions:

1. `dejaview-ssr` and `dejaview-server` are pure JVM code with zero browser
   and zero TeaVM imports (ADR-0003, ADR-0023, ADR-0024). SSG must compose the
   SSR pipeline, not fork or duplicate it.
2. Static output has no runtime routing: the file tree the build writes *is*
   the application. Anything that cannot be resolved to a written file is a
   build failure, not a 404 shipped to production.
3. Anything path-like — route paths, parameter values, asset targets, output
   directories — is untrusted before it becomes a file system path, and the
   build runs at a moment when failing loudly is cheap.
4. Hydration, state serialization and progressive streaming remain Sprint 19+
   concerns and are not faked here (ADR-0024 alpha contract).
5. The framework has no reflection and no annotation scanning (ADR-0001,
   ADR-0023), so route discovery cannot be implicit.

## Decision

### A dedicated `dejaview-ssg` module, depending on `dejaview-ssr`

`dejaview-ssg` depends only on `dejaview-ssr` (and transitively on core, DI,
server, state and HTTP). It is pure Java 25, has no browser or TeaVM imports,
and a `ModulePurityTest` fails the build if either ever lands on its classpath.
It is not a new `View` subtype and it changes no core contract: SSG is a
pipeline over the SSR contract. It is a build-time tool, not a browser
artifact, and it is not added to the playground.

### Routes are declared, never discovered

`SsgRoute` is created explicitly in Java, because the framework has no
reflection:

- `SsgRoute.page(path, lang, view)` — one static page;
- `SsgRoute.parametrized(pattern, lang, parameterSets, view)` — expands one
  whole-segment `{name}` placeholder per parameter set the application supplies
  at build time. `{}`, nested placeholders and non-whole-segment placeholders
  are rejected; parameter values may not contain `..`, `/`, whitespace or
  percent-encoded traversal.

Route paths must be dotless safe segments ending in `/path` or `404.html`-style
file pages (`SsgPaths.requireRoutePath`); `SsgRoute` validates eagerly at
declaration time, and `SsgPaths` rejects absolute paths, drive letters,
backslash traversal, `%2e`-encoded traversal and control characters. A route
therefore cannot name a file outside the output directory.

### Pipeline: plan, render, then touch the file system

`SsgGenerator.generate(site)` performs eight steps in this order:

1. **plan** — expands every route, rejects duplicate paths and output-file
   collisions (two routes writing the same file) before any rendering;
2. **render** — renders every page through `SsrRenderer.renderPage` with its
   own root injector, `ServerContext` and `PageMeta`, so routes cannot leak
   state into each other; if any render fails, the exception names the route
   and nothing has been written yet;
3. **plan-assets** — resolves asset sources, rejects missing sources, duplicate
   targets and page/asset target collisions;
4. **prepare-output** — cleans the output directory when `SsgSite.clean` is
   enabled (the default); refuses to clean a filesystem root or any directory
   containing `pom.xml`, because that is a project;
5. **copy-assets** — copies files recursively to the site;
6. **write-pages** — writes each rendered page as `index.html` under its
   route directory (`/about` → `about/index.html`, `/404.html` →
   `404.html`);
7. **sitemap** — writes `sitemap.xml` when `baseUrl` is set, excluding the
   `404.html` error document from the URL set;
8. **link-check** — validates every local `href`/`src` in the written HTML
   against the files the generator wrote (percent-decoded, external URLs and
   fragments ignored). A broken local reference fails the build with the page,
   attribute and reference that caused it.

Everything is validated eagerly, and a failure aborts the build with the
route or asset named; the output directory is never half-populated by a failed
build in a way that hides which route broke.

### Output shape and metadata

Pages carry the metadata their components wrote to `PageMeta` (`lang`,
`title`, `description`, `canonical`, `meta`/Open Graph) — identical to SSR,
plus a canonical link when `baseUrl` is configured. The output tree is
ordinary HTML plus copied assets; deploying it is a directory upload to any
static host. The generator writes pages before assets only logically — nothing
is copied until planning and rendering have succeeded.

### Alpha scope, unchanged from ADR-0024

Generated pages are complete pages, not hydrated applications: no state
serialization, no hydration markers, no progressive streaming; only the
`value` and boolean DOM property subset is representable; `script`/`style`
text is escaped; pages are written once, not streamed.

### Demo: `examples/ssg`

`examples/ssg` is a working static site, not a snippet: DejaView components
declare a home page, an about page, a catalogue, one route per catalogue entry
expanded at build time from the catalogue's slugs, and a 404 document — plus
a stylesheet copied into the output. `./mvnw -pl examples/ssg -am package`
renders it into `examples/ssg/target/site` (7 pages + 1 asset) during the
`package` phase via `exec-maven-plugin`, failing the build on any bad route,
asset or link. A Chromium Playwright test (`SsgBrowserIT`, JavaScript
disabled) asserts complete pages, a stylesheet that loads, resolvable local
links and a standalone 404 document, served from a JDK `HttpServer` bound to an
ephemeral loopback port.

## Consequences

- The acceptance criterion is met: a DejaView application generates a
  deployable static website — plain HTML, copied assets and a sitemap — with
  no server runtime and no JavaScript.
- The module composes the Sprint 16/17 pipeline instead of duplicating it;
  rendering, validation and boundaries stay in `dejaview-ssr`/`dejaview-server`.
- Static output correctness (no broken local references, no duplicate paths,
  no stale pages) is enforced at build time, where failure is cheap.
- `SsgResult` reports pages, assets, warnings and the sitemap path; a failure
  names the route or asset that caused it.
- The output is not hydrated, not incremental and not streamed; the `Writer`
  variants remain buffered (ADR-0024).
- SSG is not wired into the CLI yet (`dejaview build --static` remains future
  work, see ROADMAP Sprint 19+); applications invoke the generator from their
  own build or `exec-maven-plugin`, as `examples/ssg` demonstrates.
- The website publishes the capability (16th feature card, guide section 21 in
  both languages) and the example is validated in Chromium.

## Alternatives considered

- **Reflection/annotation scanning for route discovery.** Rejected: the
  framework is reflection-free and TeaVM-safe; explicit declaration in Java is
  the contract (ADR-0001, ADR-0023).
- **JS/HTML-sitemap templating instead of reusing the SSR renderer.**
  Rejected: SSG must inherit the SSR escaping, validation and boundary
  semantics so a page identical to its SSR counterpart is a theorem, not a
  coincidence.
- **Writing pages as they render.** Rejected: a failing render would leave a
  partial site with no indication of which route broke; the plan→render→write
  order makes failure identification exact.
- **Skipping link validation.** Rejected: a static host has no router; the
  written tree is the only truth, so local references must be checked against
  it at build time.
- **Cleaning the output directory unconditionally.** Rejected: the guard
  refuses filesystem roots and project directories (containing `pom.xml`);
  `clean(false)` opts out for hosts that manage the directory themselves.
- **Including `404.html` in `sitemap.xml`.** Rejected: the error document is
  not an indexable page; the sitemap excludes it.
- **Wiring SSG into the CLI in this sprint.** Deferred: the generator API is
  stable enough to ship; a first-class `dejaview` static command belongs with
  the hydration/tooling work, not with this module.

## Revisit triggers

- Sprint 19 (hydration) will need stable markers and serialized state in the
  emitted HTML; `SsgRenderer` call sites and `HtmlPage` are the extension
  points.
- Widening route patterns (multi-segment placeholders, `**` splats) is an API
  change and must update `SsgPaths`, `SsgRoute` and the tests together.
- Incremental/parallel generation is a performance change only after the
  all-or-nothing semantics are kept; do not trade failure atomicity away.
- A Spring Boot adapter (Sprint 21) may want SSG output served as static
  resources; the output contract (plain files + sitemap) is already
  compatible.
