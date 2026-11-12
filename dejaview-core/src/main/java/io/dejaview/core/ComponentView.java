package io.dejaview.core;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A view node that resolves to another view when rendered.
 *
 * <p>This is the rendering boundary used by components: the renderer does not
 * need to know how the view is produced, only that it can be resolved. Sprint
 * 07 builds the component lifecycle on top of this node type.</p>
 *
 * @param content view supplier, never {@code null}
 */
public record ComponentView(Supplier<View> content) implements View {

    public ComponentView {
        Objects.requireNonNull(content, "content");
    }

    /**
     * Resolves the current view.
     *
     * @throws NullPointerException if the supplier returns {@code null}
     */
    public View resolve() {
        return Objects.requireNonNull(content.get(), "component view");
    }
}
