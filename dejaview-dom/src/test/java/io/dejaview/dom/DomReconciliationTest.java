package io.dejaview.dom;

import io.dejaview.core.Views;
import io.dejaview.reactivity.Reactive;
import io.dejaview.reactivity.Signal;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomReconciliationTest {

    private final FakeDomDocument document = new FakeDomDocument();
    private final FakeDomElement container = document.createElement("main");
    private final DomRenderer renderer = new DomRenderer();

    @Test
    void textChangesUpdateTheExistingTextNode() {
        Signal<String> label = new Signal<>("Count: 0");
        MountedDom mounted = renderer.render(
                Views.component(() -> Views.p(label.get())), document, container);

        FakeDomElement paragraph = elementAt(container, 0);
        FakeText text = assertInstanceOf(FakeText.class, paragraph.children().get(0));

        label.set("Count: 1");

        assertSame(paragraph, elementAt(container, 0));
        assertSame(text, paragraph.children().get(0));
        assertEquals("Count: 1", text.text());
        mounted.dispose();
    }

    @Test
    void reactiveAttributesUpdateInPlace() {
        Signal<String> state = new Signal<>("idle");
        MountedDom mounted = renderer.render(
                Views.component(() -> Views.div().classes(state.get())), document, container);

        FakeDomElement root = elementAt(container, 0);
        assertEquals("idle", root.attributes().get("class"));

        state.set("active");

        assertSame(root, elementAt(container, 0));
        assertEquals("active", root.attributes().get("class"));
        mounted.dispose();
    }

    @Test
    void removedAttributesAreCleared() {
        Signal<Boolean> enabled = new Signal<>(true);
        MountedDom mounted = renderer.render(
                Views.component(() -> enabled.get()
                        ? Views.div().attr("data-state", "on")
                        : Views.div()),
                document,
                container);

        FakeDomElement root = elementAt(container, 0);
        assertEquals("on", root.attributes().get("data-state"));

        enabled.set(false);

        assertSame(root, elementAt(container, 0));
        assertFalse(root.attributes().containsKey("data-state"));
        mounted.dispose();
    }

    @Test
    void propertiesAndStylesUpdateInPlace() {
        Signal<String> value = new Signal<>("first");
        MountedDom mounted = renderer.render(
                Views.component(() -> Views.element("input")
                        .prop("value", value.get())
                        .style("color: " + value.get())),
                document,
                container);

        FakeDomElement input = elementAt(container, 0);
        assertEquals("first", input.properties().get("value"));
        assertEquals("color: first", input.attributes().get("style"));

        value.set("second");

        assertSame(input, elementAt(container, 0));
        assertEquals("second", input.properties().get("value"));
        assertEquals("color: second", input.attributes().get("style"));
        mounted.dispose();
    }

    @Test
    void removedElementsAreDetachedAndDisposed() {
        AtomicInteger clicks = new AtomicInteger();
        Signal<Boolean> visible = new Signal<>(true);
        MountedDom mounted = renderer.render(
                Views.component(() -> Views.div(visible.get()
                        ? Views.button("Action").onClick(clicks::incrementAndGet)
                        : Views.fragment())),
                document,
                container);

        FakeDomElement root = elementAt(container, 0);
        FakeDomElement button = assertInstanceOf(FakeDomElement.class, root.children().get(0));
        assertEquals(1, button.listenerCount("click"));

        visible.set(false);

        assertSame(root, elementAt(container, 0));
        assertTrue(root.children().isEmpty());
        assertEquals(0, button.listenerCount("click"));
        mounted.dispose();
    }

    @Test
    void nodeReplacementKeepsTheParentAndOrder() {
        Signal<Boolean> asDiv = new Signal<>(true);
        MountedDom mounted = renderer.render(
                Views.component(() -> Views.div(
                        Views.span("before"),
                        asDiv.get() ? Views.div(Views.text("content")) : Views.span("content"),
                        Views.span("after"))),
                document,
                container);

        FakeDomElement root = elementAt(container, 0);
        FakeDomElement replacement = assertInstanceOf(FakeDomElement.class, root.children().get(1));
        assertEquals("div", replacement.tagName());

        asDiv.set(false);

        assertSame(root, elementAt(container, 0));
        assertEquals(3, root.children().size());
        assertEquals("span", assertInstanceOf(FakeDomElement.class, root.children().get(0)).tagName());
        assertEquals("span", assertInstanceOf(FakeDomElement.class, root.children().get(1)).tagName());
        assertEquals("span", assertInstanceOf(FakeDomElement.class, root.children().get(2)).tagName());
        assertNotSame(replacement, root.children().get(1));
        mounted.dispose();
    }

    @Test
    void unrelatedNodesAreNotRecreated() {
        Signal<String> label = new Signal<>("first");
        MountedDom mounted = renderer.render(
                Views.component(() -> Views.div(
                        Views.span("stable"),
                        Views.p(label.get()))),
                document,
                container);

        FakeDomElement root = elementAt(container, 0);
        FakeDomElement stable = assertInstanceOf(FakeDomElement.class, root.children().get(0));
        FakeDomElement paragraph = assertInstanceOf(FakeDomElement.class, root.children().get(1));
        FakeText text = assertInstanceOf(FakeText.class, paragraph.children().get(0));

        label.set("second");

        assertSame(stable, root.children().get(0));
        assertSame(paragraph, root.children().get(1));
        assertSame(text, paragraph.children().get(0));
        assertEquals("second", text.text());
        mounted.dispose();
    }

    @Test
    void reactiveUpdatesAreBatchedIntoOneReconciliation() {
        AtomicInteger renders = new AtomicInteger();
        Signal<Integer> first = new Signal<>(0);
        Signal<Integer> second = new Signal<>(0);
        MountedDom mounted = renderer.render(
                Views.component(() -> {
                    renders.incrementAndGet();
                    return Views.p(first.get() + ":" + second.get());
                }),
                document,
                container);

        assertEquals(1, renders.get());

        Reactive.batch(() -> {
            first.set(1);
            second.set(2);
        });

        assertEquals(2, renders.get());
        assertEquals("1:2", textAt(container));
        mounted.dispose();
    }

    @Test
    void listenersAndEffectsAreCleanedUpOnDispose() {
        AtomicInteger renders = new AtomicInteger();
        Signal<Integer> count = new Signal<>(0);
        MountedDom mounted = renderer.render(
                Views.component(() -> {
                    renders.incrementAndGet();
                    return Views.button("Count: " + count.get())
                            .onClick(() -> count.update(value -> value + 1));
                }),
                document,
                container);

        FakeDomElement button = elementAt(container, 0);
        button.fire("click");
        button.fire("click");

        assertEquals("Count: 2", textInside(button));
        assertEquals(3, renders.get());
        assertEquals(1, button.listenerCount("click"));

        mounted.dispose();

        assertEquals(0, button.listenerCount("click"));
        count.set(10);
        assertEquals(3, renders.get());
    }

    @Test
    void unchangedAttributesAndPropertiesAreNotReapplied() {
        // Re-setting an unchanged attribute is observably different from leaving it
        // alone: an iframe's src reloads the frame on every identical set. The
        // renderer must skip attributes and properties whose value did not change.
        Signal<Integer> tick = new Signal<>(0);
        MountedDom mounted = renderer.render(
                Views.component(() -> Views.element("iframe")
                        .attr("src", "example.html?example=counter")
                        .attr("title", "Preview")
                        .attr("data-tick", String.valueOf(tick.get()))
                        .prop("value", "stable")
                        .classes("frame")),
                document,
                container);

        FakeDomElement frame = elementAt(container, 0);
        assertEquals(1, frame.attributeSetCount("src"));
        assertEquals(1, frame.attributeSetCount("class"));
        assertEquals(1, frame.propertySetCount("value"));

        tick.set(1);
        tick.set(2);

        assertEquals(1, frame.attributeSetCount("src"));
        assertEquals(1, frame.attributeSetCount("title"));
        assertEquals(1, frame.attributeSetCount("class"));
        assertEquals(1, frame.propertySetCount("value"));
        assertEquals(3, frame.attributeSetCount("data-tick"));
        mounted.dispose();
    }

    private static FakeDomElement elementAt(FakeDomElement parent, int index) {
        return assertInstanceOf(FakeDomElement.class, parent.children().get(index));
    }

    private static String textInside(FakeDomElement element) {
        return assertInstanceOf(FakeText.class, element.children().get(0)).text();
    }

    private static String textAt(FakeDomElement container) {
        return textInside(elementAt(container, 0));
    }
}
