# DejaView Compiler Feasibility Report

**Status:** Validated  
**Date:** 2026-11-11  
**Toolchain:** OpenJDK 25.0.5, Apache Maven 3.9.16 (Wrapper), TeaVM 0.16.0  
**Browser used for validation:** Chromium 156 headless (Playwright)

---

## 1. Question

Can DejaView compile ordinary Java source code to JavaScript that runs in a
browser, renders DOM nodes and handles user events, while keeping the compiler
replaceable behind a build boundary?

The answer is **yes**, with the constraints documented below.

---

## 2. Compiler matrix

Two TeaVM releases were tested because Maven Central's search index reported an
outdated "latest" version. The Maven metadata is authoritative.

| TeaVM | Java 21 bytecode | Java 22 | Java 23 | Java 24 | Java 25 |
|---|---|---|---|---|---|
| 0.12.0 | pass | pass | pass | pass | **fail** (`Unsupported class file major version 69`) |
| 0.16.0 | not tested | not tested | not tested | not tested | **pass** |

The Java 25 test used a class compiled with `javac --release 25` (class file
major version 69). TeaVM 0.16.0 compiled it, produced JavaScript and the result
executed correctly.

**Decision:** pin TeaVM **0.16.0** and keep `maven.compiler.release=25`.

The older 0.12.0 result is recorded because it explains why the initial
architecture document says the Java 25 support had to be validated before use.
That validation is now complete.

---

## 3. Language features exercised

The probe intentionally used modern Java features rather than a trivial
`System.out.println` program:

| Feature | Compile | Run in browser |
|---|---|---|
| `var` | pass | pass |
| Records (`record Point(int x, int y)`) | pass | pass |
| Sealed interfaces and records | pass | pass |
| Pattern matching `switch` over sealed types | pass | pass |
| Guarded patterns (`case Integer i when i > 10`) | pass | pass |
| Text blocks | pass | pass |
| Lambdas and method references | pass | pass |
| `List.of`, `String.join`, `String.isBlank` | pass | pass |

This is not a complete language/API compatibility matrix. It establishes that
the Java features used by the initial framework code are viable. New language
or JDK APIs must be checked against TeaVM's class library before use in
browser-targeted modules.

---

## 4. Maven integration

The working configuration is:

```xml
<plugin>
    <groupId>org.teavm</groupId>
    <artifactId>teavm-maven-plugin</artifactId>
    <version>${teavm.version}</version>
    <dependencies>
        <dependency>
            <groupId>org.teavm</groupId>
            <artifactId>teavm-classlib</artifactId>
            <version>${teavm.version}</version>
        </dependency>
    </dependencies>
    <executions>
        <execution>
            <phase>package</phase>
            <goals><goal>compile</goal></goals>
            <configuration>
                <mainClass>...</mainClass>
                <targetType>JAVASCRIPT</targetType>
                <jsModuleType>NONE</jsModuleType>
                <strict>true</strict>
                ...
            </configuration>
        </execution>
    </executions>
</plugin>
```

Two integration details are easy to get wrong:

1. `targetType` is the enum identifier `JAVASCRIPT`, not `javascript`.
2. TeaVM 0.16.0 requires `teavm-classlib` on the **project** class path in
   addition to the plugin dependency. It is declared with `provided` scope in
   browser modules. Without it, compilation fails with an internal
   `JSClassProcessor.getObjectClass()` null error.

`jsModuleType=NONE` produces a classic script that exposes `main` as a global
function. The example's `index.html` loads `app.js` and calls `main()`.

`strict=true` is enabled so missing classes fail the build instead of failing
later in the browser.

---

## 5. DOM and Web API interoperability

TeaVM's JSO (JavaScript Object) APIs are used:

- `org.teavm.jso.dom.html.HTMLDocument`
- `org.teavm.jso.dom.html.HTMLElement`
- `org.teavm.jso.dom.xml.Node`
- `org.teavm.jso.dom.events.EventListener`

Validation performed in Chromium:

1. `DejaView.mount(view, "app")` created elements and text nodes.
2. Attributes were applied through `setAttribute`.
3. A `click` listener was registered through `addEventListener`.
4. Clicking the button changed the DOM text content.
5. No page errors were reported.

The JSO dependency is confined to `dejaview-dom`'s browser adapter. The
renderer and the core view model have no TeaVM imports, so they run and are
tested on the JVM.

---

## 6. Limitations and risks

### 6.1 JDK class library is not the browser platform

TeaVM provides an emulated subset of the JDK. Not every `java.*` API is
available, and availability can change between TeaVM releases. Framework code
must not assume that JVM-only APIs (threads, blocking I/O, `java.net`,
`java.nio` file systems, reflection-heavy libraries) work in the browser.

Mitigations:

- Keep browser-facing modules small and explicit.
- Use `strict=true` in TeaVM builds.
- Test browser-targeted code in a real browser, not only on the JVM.
- Add a compatibility test when introducing a new JDK API.

### 6.2 Reflection and metaprogramming

TeaVM supports a limited reflection model and has its own metaprogramming API.
Code that relies on arbitrary runtime reflection, dynamic proxies or class-path
scanning will not behave like it does on the JVM.

Mitigation: prefer explicit registration and compile-time code generation.
Annotation processing runs on the JVM at build time and is a safer foundation
than runtime reflection.

### 6.3 No threads or blocking APIs in the browser

The browser is single-threaded from the application's perspective. Future HTTP,
routing and data-loading APIs must be asynchronous. This is a design constraint,
not a TeaVM bug.

### 6.4 Output size

The first example produces approximately 32 KB of unminified JavaScript plus a
source map. That includes the TeaVM runtime and the small framework surface.
Production builds should enable TeaVM's minification/optimization settings and
be measured rather than estimated.

### 6.5 JavaScript target only

WebAssembly is explicitly out of scope by project decision. TeaVM also supports
WASM targets, but DejaView must not use them. The Maven configuration fixes
`targetType=JAVASCRIPT`.

### 6.6 Browser baseline

The generated output uses modern JavaScript constructs (`let`, arrow functions,
classes). The practical baseline is an ES2015-capable browser. A formal browser
support matrix is still a project decision; the initial E2E test covers
Chromium.

### 6.7 Compiler coupling

TeaVM types currently appear in:

- the Maven plugin configuration;
- the `provided` dependencies of browser modules;
- the `dejaview-dom` browser adapter.

The core and renderer are compiler-agnostic. Replacing TeaVM would require a new
platform adapter and build configuration, not a rewrite of the view model.

---

## 7. Recommended approach

1. Keep TeaVM 0.16.0 pinned in the parent POM.
2. Keep Java 25 as the language/bytecode level.
3. Isolate JSO usage in `dejaview-dom` (and future platform modules).
4. Run JVM unit tests for renderer logic and browser E2E tests for the JSO
   boundary.
5. Re-run the compatibility matrix before upgrading TeaVM; do not assume a
   version bump is safe.
6. Add a small compatibility test whenever a new JDK API enters browser code.

---

## 8. Reproducing the validation

```bash
# Full JVM build, unit tests and TeaVM compilation
./mvnw clean verify

# Browser end-to-end test (downloads Chromium on first run)
./mvnw -Pe2e -pl examples/counter -am verify
```

The generated browser artifacts are written to
`examples/counter/target/webapp/`.
