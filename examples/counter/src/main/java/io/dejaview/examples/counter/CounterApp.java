package io.dejaview.examples.counter;

import io.dejaview.core.View;
import io.dejaview.core.Views;
import io.dejaview.dom.DejaView;
import io.dejaview.dom.Dom;

/**
 * Minimal DejaView application used to validate the Java-to-JavaScript stack.
 *
 * <p>The count is updated imperatively. Fine-grained reactivity is a later
 * milestone; this example deliberately proves only rendering, event handling
 * and DOM updates.</p>
 */
public final class CounterApp {

    private int count;

    private CounterApp() {
    }

    public static void main(String[] args) {
        new CounterApp().start();
    }

    private void start() {
        DejaView.mount(view(), "app");
        System.out.println("DejaView counter mounted");
    }

    private View view() {
        return Views.div(
                        Views.h1("DejaView"),
                        Views.p("Count: " + count).id("count"),
                        Views.button("Increment").onClick(this::increment),
                        Views.p("Java. Reimagined for the Web.").classes("tagline"))
                .classes("counter");
    }

    private void increment() {
        count++;
        Dom.byId("count").setText("Count: " + count);
    }
}
