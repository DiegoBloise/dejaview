package io.dejaview.examples.website.docs;

import java.util.List;

import static io.dejaview.examples.website.docs.Doc.*;

/**
 * English user documentation — a complete, step-by-step tutorial.
 * Portuguese lives in {@code TutorialPt}; both render through the same model.
 */
public final class TutorialEn {

    private TutorialEn() {
    }

    public static List<Block> content() {
        return List.of(

                h2("introduction", "What you'll build"),
                prose(text("This guide takes you from an empty machine to a running DejaView app — a real, reactive counter in the browser, written in pure Java 25. Along the way you'll learn signals, components, composition, dependency injection, routing and HTTP. Everything you see here is the same API used by the examples in this repository.")),
                note("tip", text("No extra build tools are required beyond the repository's Maven Wrapper — and you can be coding in under five minutes.")),

                h2("prerequisites", "1. Prerequisites"),
                prose(text("DejaView targets Java 25. Before you begin, make sure you have:")),
                bullets(
                        item(code("JDK 25"), text(" — with JAVA_HOME pointing at it (check with the command below).")),
                        item(code("Git"), text(" — to clone the repository.")),
                        item(text("Network access on the first build, to download Maven dependencies."))),
                shell(Snippets.REQUIREMENTS),
                prose(text("The output must report a 25.x runtime. On Windows, use mvnw.cmd in place of ./mvnw throughout this guide.")),

                h2("get-the-code", "2. Get the code and build it"),
                prose(text("Clone the repository and run the full build:")),
                shell(Snippets.CLONE_BUILD),
                prose(text("The build compiles every module, runs the JVM test suite and compiles the examples to JavaScript. When it finishes with BUILD SUCCESS, you have a working toolchain.")),
                note("info", text("./mvnw clean verify also exercises the compiled examples in a headless Chromium browser when you pass -Pe2e. That's optional and only needed to validate browser behavior.")),

                h2("run-the-app", "3. Run it in the browser"),
                prose(text("The counter example is the smallest complete DejaView app. Serve its compiled output with any static file server — for instance:")),
                shell(Snippets.SERVE),
                prose(text("Then open "), link("http://localhost:4280", "http://localhost:4280"), text(" and click Increment. The count updates without a single manual DOM operation — that's DejaView's reactive renderer at work.")),
                prose(text("The compiled output lives in plain static files:")),
                shell(Snippets.WEBAPP_LAYOUT),
                note("tip", strong("Any static server works"), text(" — python3 -m http.server, npx serve, nginx, or your favorite hosting. There is no application server to run in production.")),

                h2("project-layout", "4. Project layout"),
                prose(text("DejaView is a Maven multi-module project. For day-to-day work you only need to know where your code goes:")),
                bullets(
                        item(code("dejaview-core"), text(" — the immutable view model and component API you code against.")),
                        item(code("dejaview-reactivity"), text(" — Signal, Effect and Computed. No browser imports.")),
                        item(code("dejaview-dom"), text(" — the renderer and the browser adapter.")),
                        item(code("dejaview-di"), text(" — dependency injection: tokens, providers, scopes, hierarchical injectors.")),
                        item(code("dejaview-router"), text(" — client-side routing with outlets and browser history.")),
                        item(code("dejaview-http"), text(" — a typed HTTP client with interceptors and cancellation.")),
                        item(code("examples/counter"), text(" — the starter app you build above; the friendliest starting point for your own."))),
                note("info", code("dejaview-core"), text(" never imports a browser API. Everything browser-specific lives in dejaview-dom, which is what lets the same components compile and test on the JVM.")),

                h2("first-component", "5. Your first component"),
                prose(text("Open "), code("examples/counter/src/main/java/io/dejaview/examples/counter/CounterComponent.java"), text(". Here it is, in full:")),
                java(Snippets.COUNTER_COMPONENT),
                prose(text("Walk through it line by line:")),
                bullets(
                        item(code("Signal<Integer> count = new Signal<>(0)"), text(" — your reactive state, held by the component instance.")),
                        item(code("render()"), text(" — returns a tree of views. It runs once per mount, then again every time a signal it read changes.")),
                        item(code("Views.p(\"Count: \" + current)"), text(" — plain text view; the current signal value is read during render, so the renderer knows to track it.")),
                        item(code("onClick(this::increment)"), text(" — event binding. increment() calls count.update(v -> v + 1), which schedules exactly the views that read count."))),
                prose(text("The app entry point mounts the component into the element with id "), code("app"), text(":")),
                java(Snippets.JAVA_MAIN),

                h2("signals", "6. State with signals"),
                prose(text("Signals are the heart of DejaView. get() reads and tracks, set() replaces the value, update() transforms it. Computed derives a new value lazily from other signals — it recomputes only when read after a dependency changed:")),
                java(Snippets.SIGNALS_AND_COMPUTED),
                prose(text("Effects are the other half: anything inside an Effect re-runs automatically when signals it read change. The renderer uses effects internally, so views update themselves — you never poke the DOM directly.")),
                note("tip", strong("Mental model:"), text(" a signal is a cell, render() is a function of those cells, and DejaView re-runs exactly the cells that changed.")),

                h2("conditional", "7. Conditional rendering"),
                prose(text("Conditionals are just Java. A ternary inside render() chooses between views; Views.fragment lets you return multiple children from a branch:")),
                java(Snippets.CONDITIONAL),

                h2("lists", "8. Lists and keys"),
                prose(text("Build the children of a list with a loop, and give each child a stable key. DejaView reconciles keyed children by identity instead of position, which keeps DOM nodes — and their state — stable when the list reorders:")),
                java(Snippets.KEYED_LIST),
                note("warning", strong("Always key list children."), text(" Without a key, DejaView reconciles positionally — fine for append-only lists, surprising for reorders.")),

                h2("composition", "9. Composing components"),
                prose(text("Components compose through three mechanisms: inputs, outputs and slots. Here is a parent passing data down, listening to events, and projecting content into named slots:")),
                java(Snippets.INPUTS),
                prose(text("And the child receiving them:")),
                java(Snippets.CHILD),
                bullets(
                        item(strong("Inputs"), text(" — set through .input(name, value) on the component view reference; read with input(name, Type.class). Missing or mistyped inputs fail fast with a clear error.")),
                        item(strong("Outputs"), text(" — the child calls emit(\"name\", payload); the parent listens with .on(\"name\", handler). Listeners fire in registration order.")),
                        item(strong("Slots"), text(" — default content is read with slot(); named content with slot(\"name\") and projected with .slot(\"name\", view)."))),
                note("info", text("Input values are pushed by the renderer on every update, so children always see the latest values — no manual change detection needed.")),

                h2("lifecycle", "10. Lifecycle hooks"),
                prose(text("Three hooks cover the life of a component instance. Override only the ones you need:")),
                java(Snippets.LIFECYCLE),
                bullets(
                        item(code("onInit"), text(" — once per mounted node, before the first render. Good for one-time setup.")),
                        item(code("onUpdate"), text(" — after each re-render. Good for logging or integrating external widgets.")),
                        item(code("onDestroy"), text(" — when the component is removed. Release resources, timers and subscriptions here."))),

                h2("di", "11. Dependency injection"),
                prose(text("DejaView ships a small, explicit injector — no classpath scanning and no reflection. You bind tokens to providers yourself:")),
                java(Snippets.DI),
                bullets(
                        item(code("SINGLETON"), text(" is the default scope — one instance per injector, shared by its children.")),
                        item(code("TRANSIENT"), text(" creates a new instance on every resolution.")),
                        item(code("SCOPED"), text(" creates one instance per resolving injector — the request-scope equivalent."))),
                prose(text("Injectors nest: a child injector sees the parent's bindings and can shadow them. Closing an injector closes everything it created, in reverse order of creation. InjectionToken.of(MyClass.class) gives you a canonical token per class, so binding and injecting by type always agree.")),

                h2("routing", "12. Routing"),
                prose(text("Client-side routing is a single module with no mandatory wiring beyond mounting an outlet. Define routes, hand the outlet to a component, and DejaView matches URLs to components:")),
                java(Snippets.ROUTER),
                bullets(
                        item(code(":name"), text(" captures one path segment; the outlet pushes it to the matched component as an input.")),
                        item(code("**"), text(" is the catch-all — useful for home screens and 404s.")),
                        item(text("Guards, redirects, metadata, query parameters and navigation events are available on the route builder."))),
                prose(text("A parameterized screen reads its input like any other input:")),
                java(Snippets.ROUTE_PARAMS),
                note("tip", strong("Compose routers."), text(" Only one outlet is required per page region — nest outlets for nested UIs.")),

                h2("http", "13. HTTP"),
                prose(text("dejaview-http is a typed, async HTTP client. Requests are immutable, headers are case-insensitive, and responses arrive as HttpResult values — success or a structured failure, never a thrown exception for a non-2xx status:")),
                java(Snippets.HTTP_PING),
                bullets(
                        item(text("Exactly-once delivery: your callback is called exactly once per request.")),
                        item(text("Interceptors can observe or rewrite requests and responses — authentication, logging, tracing.")),
                        item(text("Timeouts, cancellation and credentials are first-class options on the builder.")),
                        item(text("JSON is handled by a small, dependency-free reader/writer — no third-party databind required."))),

                h2("forms", "14. Reactive forms"),
                prose(text("dejaview-forms adds reactive controls, groups and arrays. A control owns its value, its validation state and the interaction flags, and every read tracks a dependency:")),
                java(Snippets.FORMS_CONTROL),
                prose(text("Validators are plain functions. Synchronous validators run first; asynchronous validators only run when the synchronous result is valid, and responses for superseded values are discarded:")),
                java(Snippets.FORMS_VALIDATION),
                prose(text("Groups aggregate their children: the value is an immutable map, errors are prefixed by name, and status bubbles up. Arrays do the same with positional keys:")),
                java(Snippets.FORMS_GROUP),
                prose(text("FormBindings connects regular DOM controls to form state, so typing, checking and selecting update the control without manual listeners:")),
                java(Snippets.FORMS_BINDING),
                bullets(
                        item(code("touched"), text(" — set on blur; show validation messages from it.")),
                        item(code("dirty"), text(" — set on the first value change.")),
                        item(code("status()"), text(" — VALID, INVALID, PENDING or DISABLED, aggregated through the tree."))),
                note("tip", strong("Values are never null."), text(" Use the type's empty value — \"\", 0 or false — for an unfilled control.")),

                h2("state", "15. State management"),
                prose(text("dejaview-state lifts application state out of individual components. A StateStore is a shared, observable holder: every component that reads it observes the same value, and writes are synchronous:")),
                java(Snippets.STATE_STORE),
                prose(text("Derived state is a lazy Computed owned by the store, so reading it inside an effect tracks the projection. Selector gives the projection a stable name for diagnostics, and Stores.combine derives values across independent stores — no global store of everything:")),
                java(Snippets.STATE_DERIVED),
                prose(text("Asynchronous state is an immutable AsyncState — IDLE, LOADING, SUCCESS or FAILURE — optionally carrying the last good data while a refresh is in flight (stale-while-revalidate). AsyncStore owns the operation, a generation counter discards superseded attempts, and exactly one outcome wins per attempt:")),
                java(Snippets.STATE_ASYNC),
                prose(text("Failures are structured AsyncError values with a stable, machine-readable code — applications map codes to localized messages, exactly like form error keys. Persistence is opt-in: PersistedStore restores the value before the first observer exists, then writes every later change through to the storage you provide:")),
                java(Snippets.STATE_PERSIST),
                bullets(
                        item(code("PersistedStore"), text(" — restore from storage before the first observer exists, then write every change through. Decode and storage errors are reported as codes, never thrown at you.")),
                        item(code("StateRegistry"), text(" — an ordinary object created at the composition root, keyed by name; no global singletons.")),
                        item(code("AsyncState.data()"), text(" — the last good value, even while refreshing or after a failed refresh."))),
                note("tip", strong("Dispose at the boundary."), text(" A store owns the computeds it creates; dispose them — or a StateRegistry — when the owning view is destroyed.")),

                h2("styling", "16. Styling your app"),
                prose(text("DejaView never touches your CSS. Views carry plain class and style attributes, so any stylesheet works — hand-written CSS, Bootstrap, Tailwind CSS, daisyUI:")),
                java(Snippets.STYLING),
                prose(text("Bring your own framework: add its CSS to your page's head, use its classes in .classes(...), and you're done. DejaView ships no UI kit and no CSS pipeline on purpose.")),

                h2("production", "17. Building for production"),
                prose(text("The build you ran in step 2 is the production build. ./mvnw clean verify compiles everything to JavaScript and writes the result to each example's target/webapp directory — plain files you can host anywhere:")),
                shell(Snippets.WEBAPP_LAYOUT),
                bullets(
                        item(code("app.js"), text(" — your application, compiled.")),
                        item(code("app.js.map"), text(" — the source map for debugging.")),
                        item(code("index.html"), text(" — the entry page that loads the bundle."))),
                prose(text("Upload those files to any static host (nginx, S3, Netlify, GitHub Pages) and you have a production deployment.")),

                h2("troubleshooting", "18. Troubleshooting"),
                bullets(
                        item(code("java -version shows 8, 17 or 21"), text(" — your JAVA_HOME points at the wrong JDK. Point it at a Java 25 install and reopen the terminal.")),
                        item(code("permission denied: ./mvnw"), text(" — run chmod +x mvnw, or use mvnw.cmd on Windows.")),
                        item(code("Port 4280 already in use"), text(" — pick another port: python3 -m http.server 4281 -d examples/counter/target/webapp.")),
                        item(code("Blank page, no counter"), text(" — open the browser console. A 404 for app.js means you're serving the wrong directory; it must be examples/counter/target/webapp, not the repository root.")),
                        item(code("BUILD FAILURE downloading dependencies"), text(" — check network access; Maven needs to reach repo.maven.apache.org on the first build."))),

                h2("next-steps", "19. Next steps"),
                prose(
                        text("Explore the "),
                        link("/website/playground/", "interactive playground"),
                        text(" — every example runs the real compiled framework in your browser — then build your own app from the counter example. The full source, issues and discussions live on "),
                        link("https://github.com/dejaview/dejaview", "GitHub"),
                        text(".")));
    }
}
