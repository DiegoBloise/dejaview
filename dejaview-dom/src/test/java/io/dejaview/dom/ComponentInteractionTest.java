package io.dejaview.dom;

import io.dejaview.core.AbstractComponent;
import io.dejaview.core.ComponentBindings;
import io.dejaview.core.DejaViewException;
import io.dejaview.core.View;
import io.dejaview.core.Views;
import io.dejaview.reactivity.Signal;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentInteractionTest {

    private final FakeDomDocument document = new FakeDomDocument();
    private final FakeDomElement container = document.createElement("main");
    private final DomRenderer renderer = new DomRenderer();

    @Test
    void componentReceivesInputsOnMount() {
        MountedDom mounted = renderer.render(
                Views.componentFactory(GreetingInput::new)
                        .input("username", "Ada")
                        .input("level", 3),
                document,
                container);

        assertEquals("Ada:3", textInside(elementAt(container, 0)));
        mounted.dispose();
    }

    @Test
    void inputChangesTriggerReRenderWithNewValues() {
        Signal<String> member = new Signal<>("Ada");
        GreetingInput.instances.clear();

        MountedDom mounted = renderer.render(
                Views.component(() -> Views.componentFactory(GreetingInput::new)
                        .input("username", member.get())
                        .input("level", 1)),
                document,
                container);

        FakeDomElement first = elementAt(container, 0);
        assertEquals("Ada:1", textInside(first));

        member.set("Grace");

        assertEquals("Grace:1", textInside(elementAt(container, 0)));
        // The component instance survived the input change.
        assertEquals(1, GreetingInput.instances.size());
        assertEquals(2, GreetingInput.instances.get(0).renders);
        mounted.dispose();
    }

    @Test
    void missingRequiredInputFailsFast() {
        DejaViewException failure = assertThrows(DejaViewException.class,
                () -> renderer.render(
                        Views.componentFactory(GreetingInput::new),
                        document,
                        container));

        assertEquals("missing-input", failure.code());
        assertTrue(failure.getMessage().contains("'username'"));
    }

    @Test
    void wronglyTypedInputFailsFast() {
        DejaViewException failure = assertThrows(DejaViewException.class,
                () -> renderer.render(
                        Views.componentFactory(GreetingInput::new)
                                .input("username", 42)
                                .input("level", 1),
                        document,
                        container));

        assertEquals("invalid-input", failure.code());
    }

    @Test
    void optionalInputFallsBackToDefault() {
        MountedDom mounted = renderer.render(
                Views.componentFactory(OptionalInput::new),
                document,
                container);

        assertEquals("Ada", textInside(elementAt(container, 0)));
        mounted.dispose();
    }

    @Test
    void outputsAreDeliveredInRegistrationOrder() {
        List<String> received = new ArrayList<>();

        MountedDom mounted = renderer.render(
                Views.componentFactory(EmitterButton::new)
                        .on("select", value -> received.add("first:" + value))
                        .on("select", value -> received.add("second:" + value)),
                document,
                container);

        elementAt(elementAt(container, 0), 0).fire("click");

        assertEquals(List.of("first:Ada", "second:Ada"), received);
        mounted.dispose();
    }

    @Test
    void emittingWithNoListenersIsANoOp() {
        MountedDom mounted = renderer.render(
                Views.componentFactory(EagerEmitter::new),
                document,
                container);

        // Mount emitted the output with nobody listening; nothing blew up.
        assertEquals("ok", textInside(elementAt(container, 0)));
        mounted.dispose();
    }

    @Test
    void projectedDefaultAndNamedContentRenderInPlace() {
        MountedDom mounted = renderer.render(
                Views.componentFactory(CardShell::new)
                        .slot("header", Views.span("HEAD"))
                        .slot(Views.span("BODY")),
                document,
                container);

        FakeDomElement card = elementAt(container, 0);
        assertEquals("header", elementAt(card, 0).tagName());
        assertEquals("HEAD", textInside(elementAt(elementAt(card, 0), 0)));
        assertEquals("span", elementAt(card, 1).tagName());
        assertEquals("BODY", textInside(elementAt(card, 1)));
        mounted.dispose();
    }

    @Test
    void bindingsArePushedOnEveryRenderPass() {
        Signal<String> member = new Signal<>("Ada");
        BindingRecorder.recordings.clear();

        MountedDom mounted = renderer.render(
                Views.component(() -> Views.componentFactory(BindingRecorder::new)
                        .input("username", member.get())),
                document,
                container);

        member.set("Grace");

        assertEquals(List.of("Ada", "Grace"), BindingRecorder.recordings);
        mounted.dispose();
    }

    @Test
    void lifecycleHooksRunInMountUpdateDestroyOrder() {
        Signal<String> label = new Signal<>("before");
        List<String> events = new ArrayList<>();

        MountedDom mounted = renderer.render(
                Views.component(() -> {
                    label.get();
                    return Views.componentFactory(() -> new LifecycleProbe(events));
                }),
                document,
                container);

        label.set("after");
        mounted.dispose();

        assertEquals(
                List.of("bind", "init", "render", "bind", "render", "update", "destroy", "cleanup"),
                events);
    }

    @Test
    void reRenderUpdatesDomInPlacePreservingElements() {
        Signal<String> label = new Signal<>("before");

        MountedDom mounted = renderer.render(
                Views.component(() -> Views.p("v: " + label.get()).id("marker")),
                document,
                container);

        FakeDomElement before = elementAt(container, 0);
        assertEquals("v: before", textInside(before));

        label.set("after");

        FakeDomElement after = elementAt(container, 0);
        assertEquals("v: after", textInside(after));
        assertTrue(before == after);
        mounted.dispose();
    }

    @Test
    void onDestroyRunsBeforeOwnedResourcesAreClosed() {
        TrackedClose close = new TrackedClose();
        DestroyProbe.witness = close;

        MountedDom mounted = renderer.render(
                Views.componentFactory(DestroyProbe::new),
                document,
                container);

        mounted.dispose();

        // During onDestroy the owned resource was still open; after dispose it is closed.
        assertTrue(close.wasOpenDuringDestroy);
        assertTrue(close.closed);
    }

    @Test
    void conditionalSwapAcrossComponentClassesReplacesInstancesSafely() {
        Signal<Boolean> choice = new Signal<>(true);
        ClassA.closed.set(0);
        ClassB.closed.set(0);

        MountedDom mounted = renderer.render(
                Views.component(() -> choice.get()
                        ? Views.componentFactory(ClassA.class, ClassA::new)
                        : Views.componentFactory(ClassB.class, ClassB::new)),
                document,
                container);

        assertEquals("A", textInside(elementAt(container, 0)));

        choice.set(false);

        assertEquals("B", textInside(elementAt(container, 0)));
        assertEquals(1, ClassA.closed.get());
        assertEquals(0, ClassB.closed.get());

        choice.set(true);

        assertEquals("A", textInside(elementAt(container, 0)));
        assertEquals(1, ClassA.closed.get());
        assertEquals(1, ClassB.closed.get());

        mounted.dispose();

        assertEquals(2, ClassA.closed.get());
    }

    @Test
    void conditionalSwapWithMatchingKeysStillRespectsTokens() {
        Signal<Boolean> choice = new Signal<>(true);
        KeyedA.closed.set(0);
        KeyedB.closed.set(0);

        MountedDom mounted = renderer.render(
                Views.component(() -> choice.get()
                        ? Views.componentFactory(KeyedA.class, KeyedA::new).key("pane")
                        : Views.componentFactory(KeyedB.class, KeyedB::new).key("pane")),
                document,
                container);

        assertEquals("A", textInside(elementAt(container, 0)));

        choice.set(false);

        assertEquals("B", textInside(elementAt(container, 0)));
        // The key matched, but the class token differs: the node is replaced.
        assertEquals(1, KeyedA.closed.get());
        assertEquals(0, KeyedB.closed.get());
        mounted.dispose();
    }

    @Test
    void freshFactoriesWithMatchingTokensReuseInstances() {
        Signal<Integer> tick = new Signal<>(0);
        AtomicInteger created = new AtomicInteger();

        MountedDom mounted = renderer.render(
                Views.component(() -> {
                    tick.get();
                    return Views.div(
                            Views.componentFactory(ClassA.class, () -> {
                                created.incrementAndGet();
                                return new ClassA();
                            }),
                            Views.button("go").onClick(() -> tick.update(value -> value + 1)));
                }),
                document,
                container);

        assertEquals(1, created.get());

        FakeDomElement root = elementAt(container, 0);
        elementAt(root, 1).fire("click");

        // A fresh factory lambda per render does not recreate the component:
        // the class token is sufficient to prove identity.
        assertEquals(1, created.get());
        assertEquals("A", textInside(elementAt(root, 0)));
        mounted.dispose();
    }

    private static FakeDomElement elementAt(FakeDomElement parent, int index) {
        return assertInstanceOf(FakeDomElement.class, parent.children().get(index));
    }

    private static String textInside(FakeDomElement element) {
        return assertInstanceOf(FakeText.class, element.children().get(0)).text();
    }

    private static final class GreetingInput extends AbstractComponent {
        static final List<GreetingInput> instances = new ArrayList<>();

        int renders;

        GreetingInput() {
            instances.add(this);
        }

        @Override
        public View render() {
            renders++;
            return Views.p(input("username", String.class) + ":" + input("level", Integer.class));
        }
    }

    private static final class OptionalInput extends AbstractComponent {

        @Override
        public View render() {
            return Views.p(inputOr("username", String.class, "Ada"));
        }
    }

    private static final class EmitterButton extends AbstractComponent {

        @Override
        public View render() {
            return Views.div(
                    Views.button("emit").onClick(() -> emit("select", "Ada")));
        }
    }

    private static final class EagerEmitter extends AbstractComponent {

        @Override
        public View render() {
            emit("ready", "payload");
            return Views.p("ok");
        }
    }

    private static final class CardShell extends AbstractComponent {

        @Override
        public View render() {
            return Views.div(
                    Views.element("header").addChildren(slot("header").toArray(View[]::new)),
                    slot().get(0));
        }
    }

    private static final class BindingRecorder extends AbstractComponent {
        static final List<String> recordings = new ArrayList<>();

        @Override
        public void bind(ComponentBindings bindings) {
            super.bind(bindings);
            recordings.add((String) bindings.inputs().get("username"));
        }

        @Override
        public View render() {
            return Views.p((String) input("username", String.class));
        }
    }

    private static final class LifecycleProbe extends AbstractComponent {
        private final List<String> events;

        private LifecycleProbe(List<String> events) {
            this.events = events;
            onDispose(() -> events.add("cleanup"));
        }

        @Override
        public void bind(ComponentBindings bindings) {
            super.bind(bindings);
            events.add("bind");
        }

        @Override
        public void onInit() {
            events.add("init");
        }

        @Override
        public void onUpdate() {
            events.add("update");
        }

        @Override
        public void onDestroy() {
            events.add("destroy");
        }

        @Override
        public View render() {
            events.add("render");
            return Views.p("probe");
        }
    }

    private static final class TrackedClose implements AutoCloseable {
        boolean wasOpenDuringDestroy;
        boolean closed;

        @Override
        public void close() {
            closed = true;
        }
    }

    private static final class DestroyProbe extends AbstractComponent {
        static TrackedClose witness;

        private DestroyProbe() {
            own(witness);
        }

        @Override
        public void onDestroy() {
            witness.wasOpenDuringDestroy = !witness.closed;
        }

        @Override
        public View render() {
            return Views.p("probe");
        }
    }

    private static final class ClassA extends AbstractComponent {
        static final AtomicInteger closed = new AtomicInteger();

        private ClassA() {
            onDispose(closed::incrementAndGet);
        }

        @Override
        public View render() {
            return Views.span("A");
        }
    }

    private static final class ClassB extends AbstractComponent {
        static final AtomicInteger closed = new AtomicInteger();

        private ClassB() {
            onDispose(closed::incrementAndGet);
        }

        @Override
        public View render() {
            return Views.span("B");
        }
    }

    private static final class KeyedA extends AbstractComponent {
        static final AtomicInteger closed = new AtomicInteger();

        private KeyedA() {
            onDispose(closed::incrementAndGet);
        }

        @Override
        public View render() {
            return Views.span("A");
        }
    }

    private static final class KeyedB extends AbstractComponent {
        static final AtomicInteger closed = new AtomicInteger();

        private KeyedB() {
            onDispose(closed::incrementAndGet);
        }

        @Override
        public View render() {
            return Views.span("B");
        }
    }
}
