package io.dejaview.examples.counter;

import io.dejaview.core.AbstractComponent;
import io.dejaview.core.View;
import io.dejaview.core.Views;
import io.dejaview.reactivity.Signal;

/**
 * Class-based component that owns the counter state.
 *
 * <p>The count is a {@link Signal} held by the component instance. The renderer
 * creates one instance per mounted component node and reuses it across
 * reactive updates, so the signal keeps its value between renders. When the
 * component is removed, the mounted tree closes it and any owned resources are
 * released.</p>
 */
final class CounterComponent extends AbstractComponent {

    private final Signal<Integer> count = new Signal<>(0);

    @Override
    public String name() {
        return "Counter";
    }

    @Override
    public View render() {
        int current = count.get();
        return Views.div(
                        Views.h1("DejaView"),
                        Views.p("Count: " + current)
                                .id("count")
                                .attr("data-count", Integer.toString(current)),
                        Views.button("Increment").onClick(this::increment),
                        Views.p("Java. Reimagined for the Web.").classes("tagline"))
                .classes("counter");
    }

    private void increment() {
        count.update(value -> value + 1);
    }
}
