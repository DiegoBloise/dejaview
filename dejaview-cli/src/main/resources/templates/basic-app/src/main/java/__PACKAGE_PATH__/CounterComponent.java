package __PACKAGE__;

import io.dejaview.core.AbstractComponent;
import io.dejaview.core.View;
import io.dejaview.core.Views;
import io.dejaview.reactivity.Signal;

/**
 * Class-based component owning the counter state. The count lives in a
 * {@link Signal} held by this instance; the renderer reuses one instance
 * across reactive updates and closes it when the node leaves the tree.
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
