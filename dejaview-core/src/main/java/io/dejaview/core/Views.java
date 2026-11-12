package io.dejaview.core;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Factory methods for building view trees.
 *
 * <p>This class is a convenience layer over the records in this package. It
 * keeps application code compact without hiding the underlying immutable
 * model:</p>
 *
 * <pre>{@code
 * View view = Views.div(
 *     Views.h1("DejaView"),
 *     Views.button("Increment").onClick(() -> System.out.println("clicked"))
 * );
 * }</pre>
 *
 * <p>Convenience factories exist only for elements used by the initial
 * runtime and examples. {@link #element(String, View...)} covers every other
 * tag without requiring an ever-growing API.</p>
 */
public final class Views {

    private Views() {
    }

    public static TextView text(String value) {
        return new TextView(value);
    }

    public static FragmentView fragment(View... children) {
        return new FragmentView(java.util.List.of(children));
    }

    /**
     * Creates a stateless functional component that resolves its content when
     * rendered.
     */
    public static ComponentView component(Supplier<View> content) {
        Objects.requireNonNull(content, "content");
        return new ComponentView(() -> new FunctionalComponent(content));
    }

    /**
     * Creates a view that instantiates one component per mounted node.
     *
     * <p>Use this for class-based components. To mount an existing instance,
     * use {@link ComponentView#of(Component)} or return the instance from the
     * factory.</p>
     */
    public static ComponentView componentFactory(Supplier<? extends Component> factory) {
        return ComponentView.ofFactory(factory);
    }

    public static ElementView element(String tagName, View... children) {
        return new ElementView(tagName, java.util.List.of(), java.util.List.of(children), java.util.List.of());
    }

    public static ElementView div(View... children) {
        return element("div", children);
    }

    public static ElementView span(View... children) {
        return element("span", children);
    }

    public static ElementView span(String value) {
        return span(text(value));
    }

    public static ElementView h1(View... children) {
        return element("h1", children);
    }

    public static ElementView h1(String value) {
        return h1(text(value));
    }

    public static ElementView p(View... children) {
        return element("p", children);
    }

    public static ElementView p(String value) {
        return p(text(value));
    }

    public static ElementView button(View... children) {
        return element("button", children);
    }

    public static ElementView button(String label) {
        return button(text(label));
    }

    public static ElementView ul(View... children) {
        return element("ul", children);
    }

    public static ElementView li(View... children) {
        return element("li", children);
    }

    public static ElementView li(String value) {
        return li(text(value));
    }

    /**
     * Adapter that lets a view supplier participate in the component model.
     */
    private static final class FunctionalComponent implements Component {

        private final Supplier<View> content;

        private FunctionalComponent(Supplier<View> content) {
            this.content = content;
        }

        @Override
        public String name() {
            return "Function";
        }

        @Override
        public View render() {
            return Objects.requireNonNull(content.get(), "component view");
        }
    }
}
