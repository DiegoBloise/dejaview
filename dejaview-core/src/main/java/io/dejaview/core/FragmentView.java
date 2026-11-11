package io.dejaview.core;

import java.util.List;
import java.util.Objects;

/**
 * An ordered group of views without a corresponding DOM element.
 *
 * <p>Fragments are useful for returning multiple sibling views from a method
 * without introducing a wrapper element. Renderers append the fragment's
 * children directly to the parent.</p>
 *
 * @param children child views in render order
 */
public record FragmentView(List<View> children) implements View {

    public FragmentView {
        Objects.requireNonNull(children, "children");
        children = List.copyOf(children);
    }
}
