package io.dejaview.dom;

import io.dejaview.core.AbstractComponent;
import io.dejaview.core.ComponentView;
import io.dejaview.core.DejaViewException;
import io.dejaview.core.View;
import io.dejaview.core.Views;
import io.dejaview.reactivity.Signal;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KeyedReconciliationTest {

    private final FakeDomDocument document = new FakeDomDocument();
    private final FakeDomElement container = document.createElement("main");
    private final DomRenderer renderer = new DomRenderer();

    @Test
    void keyedListPreservesComponentIdentityAcrossReorder() {
        Signal<List<String>> order = new Signal<>(List.of("a", "b", "c"));
        ItemCard.created.set(0);
        ItemCard.closed.set(0);

        MountedDom mounted = renderer.render(
                Views.component(() -> Views.ul(
                        order.get().stream()
                                .map(id -> Views.componentFactory(ItemCard::new)
                                        .input("id", id)
                                        .key(id))
                                .toArray(View[]::new))),
                document,
                container);

        assertEquals(List.of("a", "b", "c"), textsIn(container));

        order.set(List.of("c", "a", "b"));

        assertEquals(List.of("c", "a", "b"), textsIn(container));
        // No new instance was created or disposed: keys drove the reorder.
        assertEquals(3, ItemCard.created.get());
        assertEquals(0, ItemCard.closed.get());
        mounted.dispose();
    }

    @Test
    void insertingKeyedItemMidListBuildsOnlyTheNewNode() {
        Signal<List<String>> order = new Signal<>(List.of("a", "c"));
        ItemCard.created.set(0);
        ItemCard.closed.set(0);

        MountedDom mounted = renderer.render(
                Views.component(() -> Views.ul(
                        order.get().stream()
                                .map(id -> Views.componentFactory(ItemCard::new).input("id", id).key(id))
                                .toArray(View[]::new))),
                document,
                container);

        assertEquals(2, ItemCard.created.get());

        order.set(List.of("a", "b", "c"));

        assertEquals(List.of("a", "b", "c"), textsIn(container));
        assertEquals(3, ItemCard.created.get());
        assertEquals(0, ItemCard.closed.get());
        mounted.dispose();
    }

    @Test
    void removingKeyedItemDisposesOnlyThatComponent() {
        Signal<List<String>> order = new Signal<>(List.of("a", "b", "c"));
        ItemCard.created.set(0);
        ItemCard.closed.set(0);

        MountedDom mounted = renderer.render(
                Views.component(() -> Views.ul(
                        order.get().stream()
                                .map(id -> Views.componentFactory(ItemCard::new).input("id", id).key(id))
                                .toArray(View[]::new))),
                document,
                container);

        order.set(List.of("a", "c"));

        assertEquals(List.of("a", "c"), textsIn(container));
        assertEquals(1, ItemCard.closed.get());
        mounted.dispose();
    }

    @Test
    void duplicateKeysFailFastBeforeAnyDomChange() {
        Signal<Boolean> duplicates = new Signal<>(false);
        MountedDom mounted = renderer.render(
                Views.component(() -> duplicates.get()
                        ? Views.ul(Views.li("x").key("dup"), Views.li("y").key("dup"))
                        : Views.ul(Views.li("x").key("x"))),
                document,
                container);

        FakeDomElement list = elementAt(container, 0);
        assertEquals(1, list.children().size());

        DejaViewException failure = assertThrows(DejaViewException.class, () -> duplicates.set(true));

        assertEquals("duplicate-key", failure.code());
        // The failed render pass did not corrupt the previous DOM order.
        assertEquals(1, list.children().size());
        mounted.dispose();
    }

    @Test
    void unkeyedChildrenKeepPositionalOrderAmongKeyedSiblings() {
        Signal<List<String>> fixed = new Signal<>(List.of("keep-1", "keep-2"));

        MountedDom mounted = renderer.render(
                Views.component(() -> Views.ul(
                        Views.li("first"),
                        Views.li("middle").key("stable"),
                        Views.li("last"))),
                document,
                container);

        FakeDomElement list = elementAt(container, 0);
        assertEquals(List.of("first", "middle", "last"), elementTexts(list));
        assertEquals(List.of("first", "middle", "last"), elementTexts(list));

        fixed.set(List.of("keep-1", "keep-2")); // no-op render, structure unchanged

        assertEquals(List.of("first", "middle", "last"), elementTexts(list));
        mounted.dispose();
    }

    @Test
    void keyedElementsReuseDomInstancesAcrossReorder() {
        Signal<List<String>> order = new Signal<>(List.of("a", "b"));

        MountedDom mounted = renderer.render(
                Views.component(() -> Views.ul(
                        order.get().stream()
                                .map(id -> Views.li(id).id("row-" + id).key(id))
                                .toArray(View[]::new))),
                document,
                container);

        FakeDomElement list = elementAt(container, 0);
        FakeDomElement firstBefore = elementAt(list, 0);
        FakeDomElement secondBefore = elementAt(list, 1);

        order.set(List.of("b", "a"));

        assertSame(secondBefore, elementAt(list, 0));
        assertSame(firstBefore, elementAt(list, 1));
        mounted.dispose();
    }

    @Test
    void keyChangeAtSinglePositionReplacesComponentInstance() {
        Signal<String> route = new Signal<>("home");
        ItemCard.created.set(0);
        ItemCard.closed.set(0);

        MountedDom mounted = renderer.render(
                Views.component(() -> Views.div(
                        Views.componentFactory(ItemCard::new)
                                .input("id", route.get())
                                .key(route.get()))),
                document,
                container);

        assertEquals("home", renderedText(elementAt(container, 0)));

        route.set("users/42");

        // The key changed at a single child position: the old instance is
        // disposed and a fresh one is bound, mirroring how a router outlet
        // swaps routed components.
        assertEquals("users/42", renderedText(elementAt(container, 0)));
        assertEquals(2, ItemCard.created.get());
        assertEquals(1, ItemCard.closed.get());
        mounted.dispose();
    }

    @Test
    void unchangedKeyAtSinglePositionReusesComponentInstance() {
        Signal<String> input = new Signal<>("a");
        ItemCard.created.set(0);
        ItemCard.closed.set(0);

        MountedDom mounted = renderer.render(
                Views.component(() -> Views.div(
                        Views.componentFactory(ItemCard::new)
                                .input("id", input.get())
                                .key("stable"))),
                document,
                container);

        input.set("b");

        // Same key: inputs are rebound on the existing instance.
        assertEquals("b", renderedText(elementAt(container, 0)));
        assertEquals(1, ItemCard.created.get());
        assertEquals(0, ItemCard.closed.get());
        mounted.dispose();
    }

    @Test
    void keyedComponentViewsCarryTheirKey() {
        ComponentView view = Views.componentFactory(ItemCard::new).key("card-1");

        assertEquals("card-1", view.key());
    }

    private List<String> textsIn(FakeDomElement root) {
        FakeDomElement list = elementAt(root, 0);
        return elementTexts(list);
    }

    private static List<String> elementTexts(FakeDomElement list) {
        List<String> texts = new ArrayList<>();
        for (var node : list.children()) {
            texts.add(((FakeDomElement) node).children().get(0) instanceof FakeText text
                    ? text.text()
                    : "");
        }
        return texts;
    }

    private static FakeDomElement elementAt(FakeDomElement parent, int index) {
        return (FakeDomElement) parent.children().get(index);
    }

    private static String renderedText(FakeDomElement div) {
        FakeDomElement li = elementAt(div, 0);
        return ((FakeText) li.children().get(0)).text();
    }

    private static final class ItemCard extends AbstractComponent {
        static final AtomicInteger created = new AtomicInteger();
        static final AtomicInteger closed = new AtomicInteger();

        private ItemCard() {
            created.incrementAndGet();
            onDispose(closed::incrementAndGet);
        }

        @Override
        public View render() {
            return Views.li(input("id", String.class));
        }
    }
}
