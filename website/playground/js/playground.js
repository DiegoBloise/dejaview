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
            id: 'todo',
            name: 'Todo list',
            desc: 'Inputs, outputs and keyed lists',
            implemented: false,
            sprint: 'Conceptual · Sprint 08 (planned)',
            source: [
                '// CONCEPTUAL PREVIEW — NOT IMPLEMENTED.',
                '// Sketch of the input/output component model targeted by Sprint 08.',
                '// Syntax will change; do not treat this as a specification.',
                '',
                'public final class TodoList extends AbstractComponent {',
                '    private final Signal<List<Todo>> todos =',
                '            new Signal<>(List.of());',
                '',
                '    @Override',
                '    public View render() {',
                '        return Views.section(',
                '                Views.input(InputSpec.text())',
                '                        .attr("placeholder", "What needs doing?")',
                '                        .onKeydown(Key.Enter, this::addTodo),',
                '                Views.ul(',
                '                        todos.get().stream()',
                '                                .map(todo -> Views.li(todo.label()))',
                '                                .toArray()));',
                '    }',
                '}'
            ].join('\n')
        },
        {
            id: 'routing',
            name: 'Routing',
            desc: 'Client-side routes mapped to components',
            implemented: false,
            sprint: 'Conceptual · post-Sprint 08 (planned)',
            source: [
                '// CONCEPTUAL PREVIEW — NOT IMPLEMENTED.',
                '// Routing has no approved design yet; this sketch illustrates the',
                '// shape being explored in docs/ROADMAP.md, not a plan of record.',
                '',
                'public static void main(String[] args) {',
                '    DejaView.mount(',
                '            Views.router(',
                '                    route("/", HomePage::new),',
                '                    route("/docs", DocsPage::new),',
                '                    route("/examples", ExamplesPage::new)),',
                '            "app");',
                '}'
            ].join('\n')
        },
        {
            id: 'forms',
            name: 'Forms & validation',
            desc: 'Declarative field validation with signals',
            implemented: false,
            sprint: 'Conceptual · post-Sprint 08 (planned)',
            source: [
                '// CONCEPTUAL PREVIEW — NOT IMPLEMENTED.',
                '// Forms arrive after the input/output model lands (Sprint 08).',
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
