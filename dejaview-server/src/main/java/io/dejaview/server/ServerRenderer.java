package io.dejaview.server;

import io.dejaview.core.DejaViewException;
import io.dejaview.core.Component;
import io.dejaview.core.ComponentView;
import io.dejaview.core.ElementView;
import io.dejaview.core.EventBinding;
import io.dejaview.core.FragmentView;
import io.dejaview.core.TextView;
import io.dejaview.core.View;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Renders a {@link View} tree into a platform-neutral {@link ServerTree}.
 *
 * <p>The renderer mirrors the browser mount pipeline in shape but not in
 * output:</p>
 * <ol>
 *   <li>components are created once per mounted node, in creation order;</li>
 *   <li>components whose class is classified {@link PlatformCompatibility#BROWSER_ONLY}
 *       — via the context's registry, or the {@link BrowserOnlyComponent} marker,
 *       or an explicit {@code componentType} token on the view — are rejected
 *       with a {@code browser-only-component} error, before {@code bind}/
 *       {@code onInit}/{@code render} are ever called on them;</li>
 *   <li>universal components are bound, initialized and rendered exactly once
 *       (no reactive updates happen on the server), and their rendered views are
 *       spliced into the output tree;</li>
 *   <li>element event handlers are dropped — a handler is meaningless across the
 *       serialization boundary — and the dropped event type names are reported,
 *       de-duplicated in first-seen order, on {@link ServerTree#droppedEvents()};</li>
 *   <li>every mounted component receives {@code onDestroy()} and then
 *       {@code close()}, in reverse creation order — a server render unmounts
 *       before returning. If the render or any cleanup step fails, every created
 *       component is still destroyed and closed, and all failures are attached to
 *       the thrown exception.</li>
 * </ol>
 *
 * <p>Fragments splice their children into the parent: no wrapper node is
 * emitted. The root of the render is the single produced node, or a
 * {@link ServerFragment} when the view produced several.</p>
 *
 * <p>The class is stateless; all per-render state is local to {@link #render},
 * so one renderer instance may serve any number of requests concurrently — the
 * render itself is sequential and unsynchronized, like the rest of the
 * framework.</p>
 */
public final class ServerRenderer {

    private ServerRenderer() {
    }

    /**
     * Renders {@code view} in the scope of {@code context}.
     *
     * @param view    the view tree to render; never {@code null}
     * @param context the request context whose compatibility registry classifies
     *                components; never {@code null}, must not be closed
     * @return the server tree and the list of dropped event types
     * @throws DejaViewException with code {@code browser-only-component} when the
     *                           view tree mounts a browser-only component
     * @throws DejaViewException with code {@code server-render-failed} when the
     *                           render fails and the failure is not a
     *                           {@code DejaViewException}
     */
    public static ServerTree render(View view, ServerContext context) {
        Objects.requireNonNull(view, "view");
        Objects.requireNonNull(context, "context");
        if (context.isClosed()) {
            throw new DejaViewException("closed-context", "Server context is closed", null);
        }
        RenderState state = new RenderState();
        List<ServerNode> nodes;
        try {
            nodes = walk(view, context, state);
        } catch (RuntimeException | Error failure) {
            RuntimeException wrapped = asRenderFailure(failure);
            RuntimeException closeFailure = closeCreated(state);
            if (closeFailure != null) {
                wrapped.addSuppressed(closeFailure);
            }
            throw wrapped;
        }
        RuntimeException closeFailure = closeCreated(state);
        if (closeFailure != null) {
            throw closeFailure;
        }
        ServerNode root = nodes.size() == 1 ? nodes.get(0) : new ServerFragment(nodes);
        return new ServerTree(root, List.copyOf(state.droppedEvents));
    }

    private static List<ServerNode> walk(View view, ServerContext context, RenderState state) {
        return switch (view) {
            case TextView text -> List.of(new ServerText(text.value()));
            case FragmentView fragment -> {
                List<ServerNode> nodes = new ArrayList<>();
                for (View child : fragment.children()) {
                    nodes.addAll(walk(child, context, state));
                }
                yield List.copyOf(nodes);
            }
            case ElementView element -> List.of(buildElement(element, context, state));
            case ComponentView component -> renderComponent(component, context, state);
        };
    }

    private static ServerElement buildElement(ElementView element, ServerContext context, RenderState state) {
        for (EventBinding event : element.events()) {
            state.droppedEvents.add(event.type());
        }
        List<ServerNode> children = new ArrayList<>();
        for (View child : element.children()) {
            children.addAll(walk(child, context, state));
        }
        return new ServerElement(element.tagName(), element.attributes(), element.properties(), children);
    }

    private static List<ServerNode> renderComponent(
            ComponentView component, ServerContext context, RenderState state) {
        // Defense in depth: when the view announces a class token, fail before
        // construction. When it does not, classification happens right after
        // creation, before bind/onInit/render.
        Class<?> announced = component.componentType();
        if (announced != null) {
            requireServerCompatible(announced, context);
        }
        Component instance = component.create();
        state.created.push(instance);
        requireServerCompatible(instance.getClass(), context);
        instance.bind(component.bindings());
        instance.onInit();
        state.initialized.add(instance);
        View rendered = Objects.requireNonNull(
                instance.render(), () -> "component '" + instance.getClass().getName() + "' returned null from render()");
        return walk(rendered, context, state);
    }

    private static void requireServerCompatible(Class<?> type, ServerContext context) {
        PlatformCompatibility compatibility = context.compatibility().classify(type);
        if (compatibility == PlatformCompatibility.BROWSER_ONLY) {
            throw new DejaViewException(
                    "browser-only-component",
                    "Component '" + type.getName() + "' is browser-only and cannot render on the server",
                    null);
        }
    }

    private static RuntimeException closeCreated(RenderState state) {
        RuntimeException first = null;
        while (!state.created.isEmpty()) {
            Component component = state.created.pop();
            // onDestroy only for components whose onInit completed; a component
            // rejected before or during initialization is closed, not destroyed.
            if (state.initialized.remove(component)) {
                first = closeStep(first, component::onDestroy);
            }
            first = closeStep(first, component::close);
        }
        return first;
    }

    private static RuntimeException closeStep(RuntimeException first, ThrowingAction action) {
        try {
            action.run();
        } catch (RuntimeException | Error failure) {
            RuntimeException normalized = failure instanceof RuntimeException runtime
                    ? runtime
                    : new DejaViewException("server-render-failed", failure.getMessage(), failure);
            if (first == null) {
                return normalized;
            }
            first.addSuppressed(normalized);
        }
        return first;
    }

    private interface ThrowingAction {
        void run();
    }

    private static RuntimeException asRenderFailure(Throwable failure) {
        if (failure instanceof DejaViewException dejaview) {
            return dejaview;
        }
        if (failure instanceof RuntimeException runtime) {
            return new DejaViewException("server-render-failed", runtime.getMessage(), runtime);
        }
        return new DejaViewException("server-render-failed", failure.getMessage(), failure);
    }

    /**
     * Per-render state. Local to a single {@code render()} call; never shared.
     */
    private static final class RenderState {
        private final Deque<Component> created = new ArrayDeque<>();
        private final Set<Component> initialized = new HashSet<>();
        private final Set<String> droppedEvents = new LinkedHashSet<>();
    }
}
