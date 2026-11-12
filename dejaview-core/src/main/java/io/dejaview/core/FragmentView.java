package io.dejaview.core;

import io.dejaview.core.internal.Preconditions;

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
 * @param key      optional reconciliation key, or {@code null} when unkeyed
 */
public record FragmentView(List<View> children, String key) implements View {

    public FragmentView {
        Objects.requireNonNull(children, "children");
        children = List.copyOf(children);
        Preconditions.requireKey(key, "key");
    }

    /**
     * Creates an unkeyed fragment.
     */
    public FragmentView(List<View> children) {
        this(children, null);
    }

    /**
     * Returns a copy of this fragment with the given reconciliation key.
     */
    public FragmentView key(String key) {
        return new FragmentView(children, key);
    }
}
