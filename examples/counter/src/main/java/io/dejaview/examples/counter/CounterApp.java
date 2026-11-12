package io.dejaview.examples.counter;

import io.dejaview.core.View;
import io.dejaview.core.Views;
import io.dejaview.dom.DejaView;
import io.dejaview.reactivity.Signal;

/**
 * Minimal DejaView application used to validate the Java-to-JavaScript stack.
 *
 * <p>The count is a {@link Signal}. The application mounts a component view
 * whose supplier reads the signal; the renderer tracks that read and
 * reconciles the mounted tree when the count changes. The click handler only
 * changes the signal, so no code touches the DOM directly.</p>
 */
public final class CounterApp {

    private final Signal<Integer> count = new Signal<>(0);

    private CounterApp() {
    }

    public static void main(String[] args) {
        new CounterApp().start();
    }

    private void start() {
        DejaView.mount(Views.component(this::view), "app");
        System.out.println("DejaView counter mounted");
    }

    private View view() {
        return Views.div(
                        Views.h1("DejaView"),
                        Views.p("Count: " + count.get())
                                .id("count")
                                .attr("data-count", Integer.toString(count.get())),
                        Views.button("Increment").onClick(this::increment),
                        Views.p("Java. Reimagined for the Web.").classes("tagline"))
                .classes("counter");
    }

    private void increment() {
        count.update(value -> value + 1);
    }
}
