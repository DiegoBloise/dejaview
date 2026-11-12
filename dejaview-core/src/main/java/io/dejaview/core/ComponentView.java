package io.dejaview.core;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A view node that represents a component.
 *
 * <p>The node holds a component factory rather than a component instance so the
 * renderer can create exactly one instance per mounted node. That instance
 * survives re-renders, which is what makes component state and component-owned
 * resources stable.</p>
 *
 * <p>Use {@link Views#component(Supplier)} for stateless functional
 * components and {@link Views#componentFactory(Supplier)} when every mounted
 * node should get its own class-based instance. {@link #of(Component)} mounts an
 * existing instance.</p>
 *
 * @param factory component factory, never {@code null}
 */
public record ComponentView(Supplier<? extends Component> factory) implements View {

    public ComponentView {
        Objects.requireNonNull(factory, "factory");
    }

    /**
     * Creates a view for an existing component instance.
     *
     * <p>The mounted tree owns the instance and closes it on disposal. Do not
     * share one instance between mounted nodes.</p>
     */
    public static ComponentView of(Component component) {
        Objects.requireNonNull(component, "component");
        return new ComponentView(() -> component);
    }

    /**
     * Creates a view that instantiates a component per mounted node.
     */
    public static ComponentView ofFactory(Supplier<? extends Component> factory) {
        return new ComponentView(factory);
    }

    /**
     * Creates the component instance for a mounted node.
     *
     * @throws NullPointerException if the factory returns {@code null}
     */
    public Component create() {
        return Objects.requireNonNull(factory.get(), "component");
    }
}
