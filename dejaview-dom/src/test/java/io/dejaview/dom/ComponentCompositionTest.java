package io.dejaview.dom;

import io.dejaview.core.AbstractComponent;
import io.dejaview.core.Component;
import io.dejaview.core.ComponentView;
import io.dejaview.core.View;
import io.dejaview.core.Views;
import io.dejaview.reactivity.Signal;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentCompositionTest {

    private final FakeDomDocument document = new FakeDomDocument();
    private final FakeDomElement container = document.createElement("main");
    private final DomRenderer renderer = new DomRenderer();

    @Test
    void componentInstancesAreCreatedOncePerMountedNode() {
        AtomicInteger created = new AtomicInteger();
        MountedDom mounted = renderer.render(
                Views.componentFactory(() -> {
                    created.incrementAndGet();
                    return new CounterComponent();
                }),
                document,
                container);

        assertEquals(1, created.get());
        FakeDomElement button = elementAt(container, 0);
        assertEquals("Count: 0", textInside(button));

        button.fire("click");

        assertEquals(1, created.get());
        assertEquals("Count: 1", textInside(button));
        mounted.dispose();
    }

    @Test
    void nestedComponentsRenderInHierarchy() {
        MountedDom mounted = renderer.render(
                Views.component(() -> Views.div(
                        Views.component(() -> Views.h1("Title")),
                        Views.componentFactory(CounterComponent::new))),
                document,
                container);

        FakeDomElement root = elementAt(container, 0);
        assertEquals("div", root.tagName());
        assertEquals("h1", elementAt(root, 0).tagName());
        assertEquals("button", elementAt(root, 1).tagName());
        mounted.dispose();
    }

    @Test
    void multipleInstancesHaveIndependentState() {
        MountedDom mounted = renderer.render(
                Views.div(
                        Views.componentFactory(CounterComponent::new),
                        Views.componentFactory(CounterComponent::new)),
                document,
                container);

        FakeDomElement root = elementAt(container, 0);
        FakeDomElement first = elementAt(root, 0);
        FakeDomElement second = elementAt(root, 1);

        first.fire("click");

        assertEquals("Count: 1", textInside(first));
        assertEquals("Count: 0", textInside(second));
        mounted.dispose();
    }

    @Test
    void removingAComponentClosesIt() {
        Signal<Boolean> show = new Signal<>(true);
        List<TrackedComponent> created = new ArrayList<>();

        MountedDom mounted = renderer.render(
                Views.component(() -> Views.div(show.get()
                        ? Views.componentFactory(() -> {
                            TrackedComponent component = new TrackedComponent("child");
                            created.add(component);
                            return component;
                        })
                        : Views.fragment())),
                document,
                container);

        assertEquals(1, created.size());
        assertEquals(0, created.get(0).closedCount());

        show.set(false);

        assertEquals(1, created.size());
        assertEquals(1, created.get(0).closedCount());
        assertTrue(elementAt(container, 0).children().isEmpty());
        mounted.dispose();
    }

    @Test
    void replacingAComponentWithAnotherNodeKindClosesIt() {
        Signal<Boolean> asComponent = new Signal<>(true);
        TrackedComponent component = new TrackedComponent("child");

        MountedDom mounted = renderer.render(
                Views.component(() -> asComponent.get()
                        ? ComponentView.of(component)
                        : Views.span("plain")),
                document,
                container);

        assertEquals(0, component.closedCount());

        asComponent.set(false);

        assertEquals(1, component.closedCount());
        assertEquals("span", elementAt(container, 0).tagName());
        mounted.dispose();
    }

    @Test
    void disposingMountedTreeClosesComponents() {
        TrackedComponent component = new TrackedComponent("child");

        MountedDom mounted = renderer.render(
                ComponentView.of(component), document, container);

        assertEquals(0, component.closedCount());

        mounted.dispose();

        assertEquals(1, component.closedCount());
    }

    private static FakeDomElement elementAt(FakeDomElement parent, int index) {
        return assertInstanceOf(FakeDomElement.class, parent.children().get(index));
    }

    private static String textInside(FakeDomElement element) {
        return assertInstanceOf(FakeText.class, element.children().get(0)).text();
    }

    private static final class CounterComponent extends AbstractComponent {

        private final Signal<Integer> count = new Signal<>(0);

        @Override
        public View render() {
            return Views.button("Count: " + count.get())
                    .onClick(() -> count.update(value -> value + 1));
        }
    }

    private static final class TrackedComponent extends AbstractComponent {

        private final String label;
        private final AtomicInteger closed = new AtomicInteger();

        private TrackedComponent(String label) {
            this.label = label;
            onDispose(closed::incrementAndGet);
        }

        @Override
        public View render() {
            return Views.span(label);
        }

        int closedCount() {
            return closed.get();
        }
    }
}
