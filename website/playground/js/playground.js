(function () {
    'use strict';

    /* ---- Example data: source code and compiled bundle paths ---- */
    var examples = {
        'counter': {
            name: 'Counter (implemented)',
            source: [
                "package io.dejaview.examples.showcase;",
                "",
                "import io.dejaview.core.AbstractComponent;",
                "import io.dejaview.core.View;",
                "import io.dejaview.core.Views;",
                "import io.dejaview.dom.DejaView;",
                "import io.dejaview.reactivity.Signal;",
                "",
                "public final class ShowcaseApp {",
                "    private ShowcaseApp() {}",
                "    public static void main(String[] args) { mountCounter(\"app\"); }",
                "    static final class CounterExample extends AbstractComponent {",
                "        private final Signal<Integer> count = new Signal<>(0);",
                "        @Override public View render() {",
                "            int current = count.get();",
                "            return Views.div(",
                "                Views.h1(\"Counter\"),",
                "                Views.p(\"Count: \" + current).id(\"count\").attr(\"data-count\", Integer.toString(current)),",
                "                Views.button(\"Increment\").onClick(() -> count.update(v -> v + 1))",
                "            ).classes(\"example\");",
                "        }",
                "    }",
                "}"
            ].join("\n"),
            iframeSrc: "/examples/showcase/target/webapp/index.html"
        },
        'greeting': {
            name: 'Greeting (implemented)',
            source: [
                "package io.dejaview.examples.showcase;",
                "",
                "import io.dejaview.core.View;",
                "import io.dejaview.core.Views;",
                "import io.dejaview.dom.DejaView;",
                "import io.dejaview.reactivity.Signal;",
                "",
                "public final class ShowcaseApp {",
                "    private ShowcaseApp() {}",
                "    public static void main(String[] args) { mountGreeting(\"app\"); }",
                "    static final class GreetingComponent implements Component {",
                "        private final Signal<String> name = new Signal(\"\");",
                "        @Override public View render() {",
                "            return Views.div(",
                "                Views.h1(\"Hello, \" + name.get()),",
                "                Views.button(\"Set name\").onClick(() -> name.set(\"DejaView\"))",
                "            ).classes(\"example\");",
                "        }",
                "    }",
                "}"
            ].join("\n"),
            iframeSrc: "/examples/showcase/target/webapp/index.html"
        },
        'composition': {
            name: 'Nested components (implemented)',
            source: [
                "package io.dejaview.examples.showcase;",
                "",
                "import io.dejaview.core.View;",
                "import io.dejaview.core.Views;",
                "import io.dejaview.dom.DejaView;",
                "import io.dejaview.reactivity.Signal;",
                "",
                "public final class ShowcaseApp {",
                "    private ShowcaseApp() {}",
                "    public static void main(String[] args) { mountComposition(\"app\"); }",
                "    static final class ParentComponent extends AbstractComponent {",
                "        @Override public View render() {",
                "            return Views.div(",
                "                Views.h1(\"Composition\"),",
                "                Views.componentFactory(() -> new ChildComponent()),",
                "                Views.componentFactory(() -> new ChildComponent())",
                "            );",
                "        }",
                "    }",
                "",
                "    static final class ChildComponent extends AbstractComponent {",
                "        private final Signal<Integer> clicks = new Signal<>(0);",
                "        @Override public View render() {",
                "            return Views.button(\"Clicks: \" + clicks.get()).onClick(() -> clicks.update(v -> v + 1));",
                "        }",
                "    }",
                "}"
            ].join("\n"),
            iframeSrc: "/examples/showcase/target/webapp/index.html"
        },
        'conditional': {
            name: 'Conditional rendering (implemented)',
            source: [
                "package io.dejaview.examples.showcase;",
                "",
                "import io.dejaview.core.View;",
                "import io.dejaview.core.Views;",
                "import io.dejaview.dom.DejaView;",
                "import io.dejaview.reactivity.Signal;",
                "",
                "public final class ShowcaseApp {",
                "    private ShowcaseApp() {}",
                "    public static void main(String[] args) { mountConditional(\"app\"); }",
                "    static final class ConditionalComponent extends AbstractComponent {",
                "        private final Signal<Boolean> show = new Signal<>(true);",
                "        @Override public View render() {",
                "            return Views.div(",
                "                Views.h1(\"Conditional\"),",
                "                Views.button(\"Toggle\").onClick(() -> show.update(v -> !v)),",
                "                show.get() ? Views.p(\"Visible\") : Views.fragment(Views.p(\"Hidden\"))",
                "            );",
                "        }",
                "    }",
                "}"
            ].join("\n"),
            iframeSrc: "/examples/showcase/target/webapp/index.html"
        }
        // planned examples are not shown in the list; only implemented ones
    };

    var currentExample = 'counter';

    /* ---- CodeMirror editor setup ---- */
    var editor = CodeMirror.fromTextArea(document.createElement('textarea'), {
        lineNumbers: true,
        mode: 'text/x-java',
        theme: 'default',
        keyMap: 'default'
    });
    document.body.replaceChild(editor, document.body.firstElementChild);

    editor.setValue(examples[currentExample].source);

    /* ---- UI: set active example ---- */
    document.querySelectorAll('.example-list li').forEach(function (li) {
        li.addEventListener('click', function () {
            var exName = this.getAttribute('data-example');
            if (examples.hasOwnProperty(exName)) {
                currentExample = exName;
                setActiveExample(exName);
            }
        });
    });
    function setActiveExample(exName) {
        document.querySelectorAll('.example-list li').forEach(function (li) {
            li.classList.toggle('active', li.getAttribute('data-example') === exName);
        });
        var ex = examples[exName];
        editor.setValue(ex.source);
        document.getElementById('example-info').innerHTML =
            '<p><strong>' + ex.name + '</strong> ' +
            (ex.source.includes('planned') ? '<span class="status-planned">Planned (Sprint 08+)</span>' : '<span class="status-implemented">Implemented</span>') +
            '</p><p>Select \"Run\" to execute the compiled example.</p>';

        var iframe = document.getElementById('preview-iframe');
        iframe.src = ex.iframeSrc + '?example=' + exName;
        document.getElementById('preview-frame').style.display = 'block';
    }

    /* ---- Run button ---- */
    document.getElementById('btn-run').addEventListener('click', function () {
        var iframe = document.getElementById('preview-iframe');
        if (iframe.src) {
            // Just show the iframe; the embedded app already runs.
            document.getElementById('output-panel').classList.add('hidden');
        } else {
            document.getElementById('output-panel').classList.remove('hidden');
            document.getElementById('output-panel').textContent =
                'No preview available for this example yet.';
        }
    });

    /* ---- Copy button ---- */
    document.getElementById('btn-copy').addEventListener('click', function () {
        var code = editor.getValue();
        navigator.clipboard.writeText(code).then(function () {
            var btn = document.getElementById('btn-copy');
            var orig = btn.textContent;
            btn.textContent = 'Copied!';
            setTimeout(function () { btn.textContent = orig; }, 1500);
        });
    });

    /* ---- Init ---- */
    setActiveExample(currentExample);
})();