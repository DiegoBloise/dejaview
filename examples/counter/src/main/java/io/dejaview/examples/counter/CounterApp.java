package io.dejaview.examples.counter;

import io.dejaview.core.View;
import io.dejaview.core.Views;
import io.dejaview.dom.DejaView;
import io.dejaview.dom.Dom;
import io.dejaview.reactivity.Effect;
import io.dejaview.reactivity.Signal;

/**
 * Minimal DejaView application used to validate the Java-to-JavaScript stack.
 *
 * <p>The count is a {@link Signal}. An {@link Effect} reads it and updates the
 * DOM text; the click handler only changes the signal. A renderer-managed
 * binding will replace the direct {@code Dom.byId} access in a later
 * milestone.</p>
 */
public final class CounterApp {

    private final Signal<Integer> count = new Signal<>(0);

    // Kept for future disposal; the signal also holds a strong reference.
    private Effect countText;

    private CounterApp() {
    }

    public static void main(String[] args) {
        new CounterApp().start();
    }

    private void start() {
        DejaView.mount(view(), "app");
        countText = new Effect(this::renderCount);
        System.out.println("DejaView counter mounted");
    }

    private View view() {
        return Views.div(
                        Views.h1("DejaView"),
                        Views.p("Count: " + count.get()).id("count"),
                        Views.button("Increment").onClick(this::increment),
                        Views.p("Java. Reimagined for the Web.").classes("tagline"))
                .classes("counter");
    }

    private void increment() {
        count.update(value -> value + 1);
    }

    private void renderCount() {
        Dom.byId("count").setText("Count: " + count.get());
    }
}
