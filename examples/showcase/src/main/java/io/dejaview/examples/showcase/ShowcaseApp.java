package io.dejaview.examples.showcase;

import io.dejaview.core.AbstractComponent;
import io.dejaview.core.Component;
import io.dejaview.core.View;
import io.dejaview.core.Views;
import io.dejaview.dom.DejaView;
import io.dejaview.reactivity.Signal;

/**
 * Entry point for the DejaView showcase and playground.
 *
 * <p>This class exposes one static method per example. The compiled TeaVM
 * bundle is loaded by the playground page inside an iframe with a query
 * parameter (e.g. {@code ?example=counter}). The {@link #main(String[])}
 * method reads the query parameter and dispatches to the corresponding
 * mount method.</p>
 */
public final class ShowcaseApp {

    private ShowcaseApp() {
    }

    public static void main(String[] args) {
        String example = getExampleFromQuery();
        switch (example) {
            case "greeting":
                mountGreeting("app");
                break;
            case "composition":
                mountComposition("app");
                break;
            case "conditional":
                mountConditional("app");
                break;
            case "counter":
            default:
                mountCounter("app");
                break;
        }
    }

    /**
     * Reads the {@code example} query parameter from the browser URL.
     * Runs only in the browser (TeaVM target); on the JVM it returns
     * {@code "counter"} so unit tests can run.
     */
    private static String getExampleFromQuery() {
        try {
            org.teavm.jso.browser.Window window = org.teavm.jso.browser.Window.current();
            String search = window.getLocation().getSearch();
            if (search != null && search.startsWith("?")) {
                String params = search.substring(1);
                for (String pair : params.split("&")) {
                    int eq = pair.indexOf('=');
                    if (eq > 0 && "example".equals(pair.substring(0, eq))) {
                        return pair.substring(eq + 1);
                    }
                }
            }
        } catch (Throwable ignored) {
            // On JVM or if JSO is not available, fall back to default.
        }
        return "counter";
    }

    /** Class-based counter example — the first implemented application. */
    public static void mountCounter(String hostId) {
        DejaView.mount(
                Views.componentFactory(CounterExample::new),
                hostId);
    }

    /** Functional component that owns state through a closure-captured signal. */
    public static void mountGreeting(String hostId) {
        DejaView.mount(
                Views.componentFactory(() -> new GreetingComponent()),
                hostId);
    }

    /** Nested components demonstrating composition and isolated state. */
    public static void mountComposition(String hostId) {
        DejaView.mount(
                Views.div(
                        Views.h1("Composition"),
                        Views.componentFactory(() -> new ParentComponent()),
                        Views.componentFactory(() -> new ChildComponent())),
                hostId);
    }

    /** Conditional rendering through a fragment. */
    public static void mountConditional(String hostId) {
        DejaView.mount(
                Views.div(
                        Views.h1("Conditional"),
                        Views.componentFactory(() -> new ConditionalComponent())),
                hostId);
    }

    /** Class-based counter, identical in shape to the examples/counter app. */
    private static final class CounterExample extends AbstractComponent {

        private final Signal<Integer> count = new Signal<>(0);

        @Override
        public View render() {
            int current = count.get();
            return Views.div(
                            Views.h1("Counter"),
                            Views.p("Count: " + current)
                                    .id("count")
                                    .attr("data-count", Integer.toString(current)),
                            Views.button("Increment")
                                    .onClick(() -> count.update(v -> v + 1)))
                    .classes("example");
        }
    }

    /** Functional component that owns state through a closure-captured signal. */
    private static final class GreetingComponent implements Component {

        private final Signal<String> name = new Signal<>("");

        @Override
        public View render() {
            return Views.div(
                            Views.h1("Hello, " + name.get()),
                            Views.button("Set name")
                                    .onClick(() -> name.set("DejaView")))
                    .classes("example");
        }
    }

    /** Parent renders two independent child components. */
    private static final class ParentComponent extends AbstractComponent {

        @Override
        public View render() {
            return Views.div(
                    Views.h1("Composition"),
                    Views.componentFactory(() -> new ChildComponent()),
                    Views.componentFactory(() -> new ChildComponent()));
        }
    }

    private static final class ChildComponent extends AbstractComponent {

        private final Signal<Integer> clicks = new Signal<>(0);

        @Override
        public View render() {
            return Views.button("Clicks: " + clicks.get())
                    .onClick(() -> clicks.update(v -> v + 1));
        }
    }

    /** Conditional rendering through a fragment. */
    private static final class ConditionalComponent extends AbstractComponent {

        private final Signal<Boolean> show = new Signal<>(true);

        @Override
        public View render() {
            return Views.div(
                    Views.h1("Conditional"),
                    Views.button("Toggle")
                            .onClick(() -> show.update(v -> !v)),
                    show.get()
                            ? Views.p("Visible")
                            : Views.fragment(Views.p("Hidden")));
        }
    }
}
