# AGENTS.md

Instructions for AI coding agents working on DejaView. Read this file before
changing code, the website or the documentation.

## What this repository is

DejaView is a Java-first frontend framework. Application code is written in
Java 25, compiled by TeaVM 0.16.0 to plain JavaScript, and runs directly in the
browser against native DOM APIs. There is no WebAssembly target and no
reflection-based runtime.

The official website is itself a DejaView application (`examples/website`),
compiled to `examples/website/target/webapp/app.js` and loaded by small static
shells under `website/`. The playground executes the pre-compiled showcase
bundle from `examples/showcase/target/webapp/`.

## Non-negotiable constraints

- **Java 25 + Maven Wrapper.** Use `./mvnw` (or `mvnw.cmd` on Windows). Do not
  add a second build tool.
- **JavaScript is the only browser target.** No WebAssembly, no WASM GC.
- **No reflection.** The framework and examples must work without it.
- **Keep modules platform-neutral.** `dejaview-core`, `dejaview-reactivity`,
  `dejaview-di` and the public API of the other modules must not import browser
  or TeaVM types. Browser code belongs in the dedicated TeaVM adapters.
- **The site is rendered by DejaView.** Never hand-write the page body in a
  shell: shells only declare metadata, `window.DEJAVIEW_PAGE` /
  `window.DEJAVIEW_LANG`, `#app` and the bundle loader.
- **The site is bilingual.** English lives at `/website/`, Portuguese at
  `/website/pt/`. Every user-facing string (copy, headings, buttons, captions,
  status messages, `aria-label`s, `title`s, meta descriptions) must exist in
  both languages and stay structurally mirrored.
- **Code samples stay in English.** Java sources, identifiers and comments in
  snippets/playground sources follow the code, not the locale. Prose around
  them is always translated.
- **Internal documentation is English.** `README.md`, `docs/**` and ADRs are
  English. Only the user-facing site is bilingual.
- **Never push.** Commit locally only. Pushing requires explicit authorization.
- **Local preview port is 4280.** Browser tests always bind an ephemeral
  `127.0.0.1` port; never assume 4280 in tests.

## Definition of Done: a feature is not done until the site and docs say so

When you add, change or remove a feature, API or behavior, complete **all**
applicable items below in the same change set.

### 1. Code and tests

- [ ] Implementation follows existing module boundaries and style.
- [ ] JUnit tests cover the new behavior and error paths.
- [ ] Browser-touching changes have a Playwright E2E assertion (`-Pe2e`).
- [ ] Real browser behavior was checked, not only compilation (see
      “Browser validation”).

### 2. Website (`examples/website` + `website/`)

- [ ] Feature card added/updated in `HomePage.features()` when the feature is
      user-visible on the landing page.
- [ ] A tutorial section exists in **both** `TutorialEn` and `TutorialPt`
      (same heading ids, same block structure), using `Snippets` constants that
      mirror code which actually compiles.
- [ ] UI copy, captions, statuses and ARIA labels use `Lang.t(...)` or paired
      `nameEn`/`namePt` fields — never an English-only literal.
- [ ] Any new interactive behavior is available in the playground when it makes
      sense: add it to `Examples.ALL`, wire the real example in
      `examples/showcase`, and cover it in `WebsiteBrowserIT`.
- [ ] Static shells (`website/index.html`, `website/pt/index.html`, and the
      `get-started` / `playground` mirrors) keep correct `lang`, title and meta
      description.
- [ ] Site CSS lives in `website/css/styles.css`; playground-only rules live in
      `website/playground/css/playground.css`. Do not leak page-specific styles
      into the shared stylesheet.

### 3. Project documentation

- [ ] `docs/IMPLEMENTATION_STATUS.md`: move/add the capability row, update
      limitations and the next-milestone note.
- [ ] `docs/ROADMAP.md`: tick delivered items and update the current sprint.
- [ ] `README.md`: capability table, module table, known limitations and quick
      start when affected.
- [ ] `docs/ARCHITECTURE.md`: update when module boundaries or contracts change.
- [ ] New architectural decision → new ADR in `docs/ADR/` (never change a past
      ADR silently; supersede it).
- [ ] `docs/COMPILER.md`: update when TeaVM constraints or bindings change.

### 4. Verification

```bash
# Full gate: unit tests + TeaVM output + artifact checks
./mvnw clean verify

# Browser E2E (Chromium via Playwright)
./mvnw -Pe2e -pl dejaview-compiler,dejaview-cli,examples/counter,examples/showcase,examples/website -am verify
```

- [ ] Both commands pass.
- [ ] No new console errors in the browser.
- [ ] English and Portuguese pages were checked, including the language switch.

## Where to add what

| Change | Files |
|---|---|
| Landing-page feature | `examples/website/.../home/HomePage.java` (`features()`, `Icons`) |
| Shared header/footer copy | `examples/website/.../component/SiteChrome.java` + `Lang` |
| Tutorial section (EN) | `examples/website/.../docs/TutorialEn.java` |
| Tutorial section (PT) | `examples/website/.../docs/TutorialPt.java` |
| Tutorial code sample | `examples/website/.../docs/Snippets.java` (copy real code) |
| New docs block type | `examples/website/.../docs/Doc.java`, `BlockView.java` |
| Playground example | `examples/website/.../playground/Examples.java`, `examples/showcase/.../ShowcaseApp.java` |
| Site shell metadata | `website/index.html`, `website/get-started.html`, `website/playground/index.html` and `website/pt/**` |
| Site design tokens/layout | `website/css/styles.css` |
| Playground layout | `website/playground/css/playground.css` |
| Syntax highlighting | `website/js/highlight.js` |
| Browser test coverage | `examples/website/src/test/java/.../WebsiteBrowserIT.java` |

### Adding a tutorial section

1. Add the block(s) to `TutorialEn.content()` and `TutorialPt.content()` at the
   same position, with the same heading id.
2. Put multi-line Java/shell samples in `Snippets`, never inline in the tutorial.
3. If the section demonstrates a runnable feature, also add or update the
   matching playground example and E2E assertion.

### Adding a playground example

1. Add the source to `examples/showcase` and verify it compiles and runs with
   `?example=<id>`.
2. Add its metadata (`id`, EN/PT name and description, source) to
   `Examples.ALL`.
3. Update the EN and PT playground `noscript` example lists in the shells.
4. Extend `WebsiteBrowserIT` so the example is listed and can be executed.

## Browser validation

The site must be served from the repository root so its absolute asset paths
work:

```bash
./mvnw -pl examples/showcase,examples/website -am package
python3 -m http.server 4280
```

Open:

- <http://localhost:4280/website/> and <http://localhost:4280/website/pt/>
- <http://localhost:4280/website/get-started.html> and the `pt/` mirror
- <http://localhost:4280/website/playground/> and the `pt/` mirror

Audit at least:

- no console errors and no failed requests;
- language switch points at the equivalent page in the other locale;
- in-page anchors land below the sticky header;
- 390 px viewport has no horizontal scroll;
- every playground example loads, runs and produces output;
- the editor highlights, accepts typing, Tab insertion and copy.

Use `curl` for HTTP status checks and a real Chromium session for anything
visual. Compilation alone does not prove the UI works.

## Commits

Use a `type: summary` message in the repository's existing style
(`feat:`, `fix:`, `test:`, `docs:`, `refactor:`, `chore:`). Keep commits focused
and mention user-visible behavior when applicable. **Do not push.**

## Source of truth

- Implemented vs planned: `docs/IMPLEMENTATION_STATUS.md`.
- Sprint order and next work: `docs/ROADMAP.md`.
- Architecture and invariants: `docs/ARCHITECTURE.md` and `docs/ADR/`.
- Public narrative and quick start: `README.md`.
- User-facing truth: the bilingual website.
