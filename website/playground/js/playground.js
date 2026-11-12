/**
 * DejaView Playground.
 *
 * Examples are executed from the pre-compiled TeaVM bundle
 * (/examples/showcase/target/webapp/index.html?example=<name>). The Java
 * sources shown here are the exact sources compiled into that bundle, kept
 * in examples/showcase — do not drift apart.
 */
(function () {
    'use strict';

    var EXAMPLES = [
        {
            id: 'counter',
            name: 'Counter',
            desc: 'Class-based component with an incrementable signal',
            implemented: true,
            sprint: 'Sprint 04–07 · implemented',
            source: [
                '// examples/showcase/src/main/java/io/dejaview/examples/showcase/ShowcaseApp.java',
                '// The same component shape as examples/counter.',
                '',
                'private static final class CounterExample extends AbstractComponent {',
                '',
                '    private final Signal<Integer> count = new Signal<>(0);',
                '',
                '    @Override',
                '    public View render() {',
                '        int current = count.get();',
                '        return Views.div(',
                '                        Views.h1("Counter"),',
                '                        Views.p("Count: " + current)',
                '                                .id("count")',
                '                                .attr("data-count", Integer.toString(current)),',
                '                        Views.button("Increment")',
                '                                .onClick(() -> count.update(v -> v + 1)))',
                '                .classes("example");',
                '    }',
                '}'
            ].join('\n')
        },
        {
            id: 'greeting',
            name: 'Greeting',
            desc: 'Functional component — state captured in the supplier closure',
            implemented: true,
            sprint: 'Sprint 07 · implemented',
            source: [
                '// Functional components: the supplier is re-invoked on every render;',
                '// state captured in the closure survives across renders.',
                '',
                'public static void mountGreeting(String hostId) {',
                '    Signal<String> name = new Signal<>("");',
                '    DejaView.mount(Views.component(() ->',
                '            Views.div(',
                '                    Views.h1("Hello, " + name.get()),',
                '                    Views.button("Set name")',
                '                            .onClick(() -> name.set("DejaView")))',
                '                    .classes("example")),',
                '            hostId);',
                '}'
            ].join('\n')
        },
        {
            id: 'composition',
            name: 'Component composition',
            desc: 'Parent renders two children with isolated state',
            implemented: true,
            sprint: 'Sprint 07 · implemented',
            source: [
                '// examples/showcase/src/main/java/io/dejaview/examples/showcase/ShowcaseApp.java',
                '',
                'private static final class ParentComponent extends AbstractComponent {',
                '',
                '    @Override',
                '    public View render() {',
                '        return Views.div(',
                '                Views.h1("Composition"),',
                '                Views.componentFactory(ChildComponent::new),',
                '                Views.componentFactory(ChildComponent::new));',
                '    }',
                '}',
                '',
                'private static final class ChildComponent extends AbstractComponent {',
                '',
                '    private final Signal<Integer> clicks = new Signal<>(0);',
                '',
                '    @Override',
                '    public View render() {',
                '        return Views.button("Clicks: " + clicks.get())',
                '                .onClick(() -> clicks.update(v -> v + 1));',
                '    }',
                '}'
            ].join('\n')
        },
        {
            id: 'conditional',
            name: 'Conditional rendering',
            desc: 'Fragments and ternaries inside a single component',
            implemented: true,
            sprint: 'Sprint 05–07 · implemented',
            source: [
                '// examples/showcase/src/main/java/io/dejaview/examples/showcase/ShowcaseApp.java',
                '',
                'private static final class ConditionalComponent extends AbstractComponent {',
                '',
                '    private final Signal<Boolean> show = new Signal<>(true);',
                '',
                '    @Override',
                '    public View render() {',
                '        return Views.div(',
                '                Views.h1("Conditional"),',
                '                Views.button("Toggle")',
                '                        .onClick(() -> show.update(v -> !v)),',
                '                show.get()',
                '                        ? Views.p("Visible")',
                '                        : Views.fragment(Views.p("Hidden")));',
                '    }',
                '}'
            ].join('\n')
        },
        {
            id: 'usercard',
            name: 'Inputs, outputs & slots',
            desc: 'Parent pushes inputs, listens to outputs, projects named slots',
            implemented: true,
            sprint: 'Sprint 08 · implemented',
            source: [
                '// examples/showcase/src/main/java/io/dejaview/examples/showcase/ShowcaseApp.java',
                '',
                'private static final class UserCardDemo extends AbstractComponent {',
                '',
                '    private final Signal<String> member = new Signal<>("Ada");',
                '    private final Signal<String> lastEvent = new Signal<>("—");',
                '',
                '    @Override',
                '    public View render() {',
                '        String name = member.get();',
                '        boolean ada = name.equals("Ada");',
                '        return Views.div(',
                '                Views.h1("Team directory"),',
                '                Views.button("Switch member")',
                '                        .onClick(() -> member.set(ada ? "Grace" : "Ada")),',
                '                Views.componentFactory(UserCard.class, UserCard::new)',
                '                        .input("username", name)',
                '                        .on("select", selected -> lastEvent.set("Selected " + selected))',
                '                        .slot("header", Views.strong(ada ? "Core team" : "Guest"))',
                '                        .slot(Views.p(ada ? "Writes the renderer." : "Reviews every PR.")),',
                '                Views.p("Event: " + lastEvent.get()).id("event-log"));',
                '    }',
                '}',
                '',
                'private static final class UserCard extends AbstractComponent {',
                '',
                '    @Override',
                '    public void onInit() {',
                '        // Invoked once per mounted node, before the first render.',
                '    }',
                '',
                '    @Override',
                '    public View render() {',
                '        String username = input("username", String.class);',
                '        View bio = slot().isEmpty() ? Views.p("No bio yet.") : slot().get(0);',
                '        return Views.div(',
                '                        Views.element("header")',
                '                                .addChildren(slot("header").toArray(View[]::new)),',
                '                        Views.h3("@" + username),',
                '                        bio,',
                '                        Views.button("Select " + username)',
                '                                .onClick(() -> emit("select", username)))',
                '                .classes("usercard");',
                '    }',
                '}'
            ].join('\n')
        },
        {
            id: 'list',
            name: 'Keyed list',
            desc: 'Keyed reconciliation keeps DOM nodes stable across reorders',
            implemented: true,
            sprint: 'Sprint 08 · implemented',
            source: [
                '// examples/showcase/src/main/java/io/dejaview/examples/showcase/ShowcaseApp.java',
                '',
                'private static final class KeyedListDemo extends AbstractComponent {',
                '',
                '    private final Signal<List<String>> order =',
                '            new Signal<>(List.of("Alpha", "Beta", "Gamma"));',
                '',
                '    @Override',
                '    public View render() {',
                '        List<View> rows = new ArrayList<>();',
                '        for (String label : order.get()) {',
                '            rows.add(Views.li(label).key(label));',
                '        }',
                '        return Views.div(',
                '                Views.h1("Keyed list"),',
                '                Views.button("Rotate").onClick(this::rotate),',
                '                Views.ul(rows.toArray(View[]::new)));',
                '    }',
                '',
                '    private void rotate() {',
                '        List<String> current = new ArrayList<>(order.get());',
                '        current.add(0, current.remove(current.size() - 1));',
                '        order.set(List.copyOf(current));',
                '    }',
                '}'
            ].join('\n')
        },
        {
            id: 'router',
            name: 'Routing',
            desc: 'Client-side routes with parameters, history and outlets',
            implemented: true,
            sprint: 'Sprint 10 · implemented',
            source: [
                '// examples/showcase/src/main/java/io/dejaview/examples/showcase/ShowcaseApp.java',
                '',
                '/** Nested routing with browser history, parameters and back/forward. */',
                'public static void mountRouter(String hostId) {',
                '    AtomicReference<Router> ref = new AtomicReference<>();',
                '    Router router = Router.builder()',
                '            .routes(',
                '                    Route.path("/users/:id")',
                '                            .component(() -> new RouterUserScreen(ref.get()))',
                '                            .metadata("screen", "user")',
                '                            .build(),',
                '                    Route.path("/about")',
                '                            .component(() -> new RouterAboutScreen(ref.get()))',
                '                            .metadata("screen", "about")',
                '                            .build(),',
                '                    // Catch-all home: the example also works when served from',
                '                    // an arbitrary static path such as /index.html.',
                '                    Route.path("**")',
                '                            .component(() -> new RouterHomeScreen(ref.get()))',
                '                            .metadata("screen", "home")',
                '                            .build())',
                '            .history(RouterHistory.browser())',
                '            .build();',
                '    ref.set(router);',
                '    DejaView.mount(Views.componentFactory(() -> new RouterShell(router)), hostId);',
                '}',
                '',
                'private static final class RouterShell extends AbstractComponent {',
                '',
                '    private final Router router;',
                '',
                '    RouterShell(Router router) {',
                '        this.router = router;',
                '    }',
                '',
                '    @Override',
                '    public View render() {',
                '        return Views.div(',
                '                Views.h1("Router"),',
                '                Views.componentFactory(() -> new RouterOutlet(router)))',
                '                .classes("example");',
                '    }',
                '}',
                '',
                '/** Parameterized screen; the outlet pushes {@code id} as an input. */',
                'private static final class RouterUserScreen extends AbstractComponent {',
                '',
                '    private final Router router;',
                '',
                '    RouterUserScreen(Router router) {',
                '        this.router = router;',
                '    }',
                '',
                '    @Override',
                '    public View render() {',
                '        String id = input("id", String.class);',
                '        return Views.div(',
                '                Views.h2("User profile").id("router-user"),',
                '                Views.p("User id: " + id).id("router-user-id"),',
                '                Views.button("Home")',
                '                        .id("router-home-btn")',
                '                        .onClick(() -> router.navigate("/")),',
                '                Views.button("About")',
                '                        .id("router-user-about")',
                '                        .onClick(() -> router.navigate("/about")));',
                '    }',
                '}'
            ].join('\n')
        },
        {
            id: 'http',
            name: 'HTTP client',
            desc: 'Fetches a static resource through HttpClient and renders the body',
            implemented: true,
            sprint: 'Sprint 11 · implemented',
            source: [
                '// examples/showcase/src/main/java/io/dejaview/examples/showcase/ShowcaseApp.java',
                '',
                '/** HTTP client demo: button triggers GET ping.json; result is rendered reactively. */',
                'private static final class HttpPingDemo extends AbstractComponent {',
                '',
                '    private final Signal<String> result = new Signal<>("—");',
                '    private final HttpClient client = HttpClient.builder().build();',
                '',
                '    @Override',
                '    public View render() {',
                '        return Views.div(',
                '                        Views.h1("HTTP"),',
                '                        Views.button("GET ping.json")',
                '                                .id("http-ping")',
                '                                .onClick(this::ping),',
                '                        Views.p(result.get()).id("http-result"))',
                '                .classes("example");',
                '    }',
                '',
                '    private void ping() {',
                '        client.get("ping.json", this::renderOutcome);',
                '    }',
                '',
                '    private void renderOutcome(HttpResult outcome) {',
                '        if (outcome instanceof HttpResult.Success success) {',
                '            result.set(success.response().body());',
                '        } else {',
                '            result.set("error: " + outcome.failure().code());',
                '        }',
                '    }',
                '}'
            ].join('\n')
        },
        {
            id: 'forms',
            name: 'Forms & validation',
            desc: 'Declarative field validation with signals',
            implemented: false,
            sprint: 'Conceptual · planned',
            source: [
                '// CONCEPTUAL PREVIEW — NOT IMPLEMENTED.',
                '// A forms API is on the roadmap; the component model it builds on',
                '// (inputs, outputs, slots, lifecycle) landed in Sprint 08.',
                '',
                'public final class SignupForm extends AbstractComponent {',
                '    private final Signal<String> email = new Signal<>("");',
                '    private final Computed<Boolean> valid =',
                '            Computed.of(() -> email.get().contains("@"));',
                '',
                '    @Override',
                '    public View render() {',
                '        return Views.form(',
                '                Views.input(InputSpec.email())',
                '                        .bind(email),',
                '                Views.button("Sign up")',
                '                        .attr("disabled", valid.get() ? null : "disabled")',
                '                        .onClick(this::submit));',
                '    }',
                '}'
            ].join('\n')
        }
    ];

    var EXAMPLE_MAP = {};
    for (var i = 0; i < EXAMPLES.length; i++) {
        EXAMPLE_MAP[EXAMPLES[i].id] = EXAMPLES[i];
    }

    var list = document.getElementById('example-list');
    var iframe = document.getElementById('preview-iframe');
    var statusLine = document.getElementById('preview-status');
    var banner = document.getElementById('conceptual-banner');
    var input = document.getElementById('editor-input');
    var highlightCode = document.getElementById('highlight-code');
    var pre = highlightCode.parentElement;
    var runButton = document.getElementById('btn-run');
    var copyButton = document.getElementById('btn-copy');
    var currentLabel = document.getElementById('current-example');

    var current = null;

    /* ---------- Sidebar construction ---------- */
    EXAMPLES.forEach(function (example) {
        var item = document.createElement('li');
        var button = document.createElement('button');
        button.type = 'button';
        button.className = 'example-item';
        button.setAttribute('data-example', example.id);
        button.setAttribute('aria-pressed', 'false');
        button.innerHTML =
            '<span class="example-item-top">' +
                '<span class="example-item-name">' + example.name + '</span>' +
                '<span class="pill ' + (example.implemented ? 'pill-ok' : 'pill-planned') + '">' +
                    (example.implemented ? 'Implemented' : 'Planned') +
                '</span>' +
            '</span>' +
            '<span class="example-item-desc">' + example.desc + '</span>';
        button.addEventListener('click', function () {
            selectExample(example.id);
        });
        item.appendChild(button);
        list.appendChild(item);
    });

    /* ---------- Editor ---------- */
    function renderHighlight() {
        var source = input.value;
        var html = window.dejaviewHighlight.java(source);
        if (source.endsWith('\n')) {
            html += ' ';
        }
        highlightCode.innerHTML = html;
        pre.scrollTop = input.scrollTop;
        pre.scrollLeft = input.scrollLeft;
    }

    input.addEventListener('input', renderHighlight);
    input.addEventListener('scroll', function () {
        pre.scrollTop = input.scrollTop;
        pre.scrollLeft = input.scrollLeft;
    });
    input.addEventListener('keydown', function (event) {
        if (event.key === 'Tab') {
            event.preventDefault();
            var start = input.selectionStart;
            var end = input.selectionEnd;
            input.setRangeText('    ', start, end, 'end');
            renderHighlight();
        }
    });

    /* ---------- Example selection ---------- */
    function selectExample(id) {
        var example = EXAMPLE_MAP[id];
        if (!example) { return; }
        current = example;

        list.querySelectorAll('.example-item').forEach(function (button) {
            var active = button.getAttribute('data-example') === id;
            button.classList.toggle('active', active);
            button.setAttribute('aria-pressed', active ? 'true' : 'false');
        });

        currentLabel.textContent = example.id + ' · ' + example.sprint;
        input.value = example.source;
        renderHighlight();

        if (example.implemented) {
            banner.hidden = true;
            iframe.hidden = false;
            runButton.disabled = false;
            statusLine.textContent = 'Ready — press Run to execute the compiled example.';
        } else {
            banner.hidden = false;
            iframe.hidden = true;
            runButton.disabled = true;
            statusLine.textContent = 'Conceptual preview — not executable yet.';
        }
    }

    /* ---------- Run ---------- */
    iframe.addEventListener('load', function () {
        if (current && current.implemented && iframe.src !== 'about:blank') {
            statusLine.textContent = 'Running \'' + current.id + '\' — compiled by TeaVM, executed for real.';
        }
    });

    runButton.addEventListener('click', function () {
        if (!current || !current.implemented) { return; }
        statusLine.textContent = 'Loading compiled example \'' + current.id + '\'…';
        iframe.src = '/examples/showcase/target/webapp/index.html?example=' + encodeURIComponent(current.id);
    });

    /* ---------- Copy ---------- */
    copyButton.addEventListener('click', function () {
        navigator.clipboard.writeText(input.value).then(function () {
            var original = copyButton.textContent;
            copyButton.textContent = 'Copied';
            setTimeout(function () { copyButton.textContent = original; }, 1600);
        });
    });

    selectExample('counter');
})();
