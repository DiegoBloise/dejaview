package io.dejaview.server;

import io.dejaview.core.AbstractComponent;
import io.dejaview.core.ComponentBindings;
import io.dejaview.core.ComponentView;
import io.dejaview.core.DejaViewException;
import io.dejaview.core.View;
import io.dejaview.core.Views;
import io.dejaview.di.Injector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerRendererTest {

    private Injector root;
    private ServerContext context;

    @BeforeEach
    void setUp() {
        root = Injector.builder().name("server-test").build();
        context = ServerContext.create(root, ServerRequest.of("GET", "/"));
    }

    @AfterEach
    void tearDown() {
        context.close();
        root.close();
    }

    @Test
    void rendersTextToServerText() {
        ServerTree tree = ServerRenderer.render(Views.text("hello"), context);

        assertEquals(new ServerText("hello"), tree.root());
        assertEquals(List.of(), tree.droppedEvents());
        assertTrue(tree.isComplete());
    }

    @Test
    void rendersElementsWithAttributesPropertiesAndChildrenInOrder() {
        ServerTree tree = ServerRenderer.render(
                Views.div()
                        .attr("class", "card")
                        .attr("id", "main")
                        .prop("value", "abc")
                        .addChildren(Views.h1("Title"), Views.p("Body")),
                context);

        ServerElement root = assertInstanceOf(ServerElement.class, tree.root());
        assertEquals("div", root.tagName());
        assertEquals(List.of(
                        new io.dejaview.core.Attribute("class", "card"),
                        new io.dejaview.core.Attribute("id", "main")),
                root.attributes());
        assertEquals(List.of(new io.dejaview.core.Property("value", "abc")), root.properties());
        assertEquals(2, root.children().size());
        assertEquals("Title", assertInstanceOf(ServerText.class,
                assertInstanceOf(ServerElement.class, root.children().get(0)).children().get(0)).value());
        assertInstanceOf(ServerElement.class, root.children().get(1));
    }

    @Test
    void dropsEventHandlersAndReportsDeduplicatedTypesInFirstSeenOrder() {
        ServerTree tree = ServerRenderer.render(
                Views.button("Save")
                        .on("click", () -> { })
                        .on("click", () -> { })
                        .on("input", () -> { }),
                context);

        ServerElement root = assertInstanceOf(ServerElement.class, tree.root());
        assertEquals(List.of("click", "input"), tree.droppedEvents());
    }

    @Test
    void spliceFragmentsWithoutEmittingWrapperNodes() {
        ServerTree tree = ServerRenderer.render(
                Views.element("section").addChildren(
                        new io.dejaview.core.FragmentView(List.of(Views.text("a"), Views.text("b"))),
                        Views.text("c")),
                context);

        ServerElement root = assertInstanceOf(ServerElement.class, tree.root());
        assertEquals(
                List.of(new ServerText("a"), new ServerText("b"), new ServerText("c")),
                root.children());
    }

    @Test
    void multipleRootLevelNodesAreWrappedInAFragment() {
        ServerTree tree = ServerRenderer.render(
                new io.dejaview.core.FragmentView(List.of(Views.p("one"), Views.p("two"))),
                context);

        ServerFragment root = assertInstanceOf(ServerFragment.class, tree.root());
        assertEquals(2, root.children().size());
    }

    @Test
    void universalComponentIsMountedAndRenderedOnce() {
        List<String> log = new ArrayList<>();
        ServerTree tree = ServerRenderer.render(
                Views.componentFactory(() -> new LifecycleComponent("hero", log)),
                context);

        ServerElement root = assertInstanceOf(ServerElement.class, tree.root());
        assertEquals("hero", root.tagName());
        // bind -> onInit -> render -> onDestroy -> close
        assertEquals(List.of("bind:hero", "init:hero", "render:hero", "destroy:hero", "close:hero"), log);
        assertEquals(List.of(), tree.droppedEvents());
    }

    @Test
    void mountedComponentReceivesItsBindings() {
        InputReadingComponent component = new InputReadingComponent();
        ServerTree tree = ServerRenderer.render(
                Views.componentFactory(() -> component).input("username", "ada"),
                context);

        ServerElement root = assertInstanceOf(ServerElement.class, tree.root());
        assertEquals("ada",
                assertInstanceOf(ServerText.class, root.children().get(0)).value());
    }

    @Test
    void nestedComponentsCloseInReverseCreationOrder() {
        List<String> log = new ArrayList<>();
        ServerTree tree = ServerRenderer.render(
                Views.componentFactory(() -> new OuterComponent(log)),
                context);

        assertEquals(
                List.of(
                        "bind:outer", "init:outer", "render:outer",
                        "bind:inner", "init:inner", "render:inner",
                        "destroy:inner", "close:inner",
                        "destroy:outer", "close:outer"),
                log);
    }

    @Test
    void browserOnlyComponentIsRejectedBeforeLifecycleRuns() {
        List<String> log = new ArrayList<>();

        DejaViewException failure = assertThrows(DejaViewException.class,
                () -> ServerRenderer.render(
                        Views.componentFactory(() -> new MarkerComponent(log)),
                        context));

        assertEquals("browser-only-component", failure.code());
        assertEquals(List.of("close:marker"), log,
                "bind/init/render must not run; close() still releases created resources");
    }

    @Test
    void browserOnlyClassTokenRejectsConstruction() {
        java.util.concurrent.atomic.AtomicInteger constructed =
                new java.util.concurrent.atomic.AtomicInteger();

        DejaViewException failure = assertThrows(DejaViewException.class,
                () -> ServerRenderer.render(
                        ComponentView.ofFactory(MarkerComponent.class, () -> {
                            constructed.incrementAndGet();
                            return new MarkerComponent(new ArrayList<>());
                        }),
                        context));

        assertEquals("browser-only-component", failure.code());
        assertEquals(0, constructed.get(), "browser-only token must fail before the factory runs");
    }

    @Test
    void thirdPartyComponentCanBeFlaggedBrowserOnlyThroughTheRegistry() {
        List<String> log = new ArrayList<>();
        context.compatibility().register(ThirdPartyComponent.class, PlatformCompatibility.BROWSER_ONLY);

        DejaViewException failure = assertThrows(DejaViewException.class,
                () -> ServerRenderer.render(
                        Views.componentFactory(() -> new ThirdPartyComponent(log)),
                        context));

        assertEquals("browser-only-component", failure.code());
        assertEquals(List.of("close:third-party"), log);
    }

    @Test
    void registryOverrideWinsOverMarkerInterface() {
        List<String> log = new ArrayList<>();
        context.compatibility().register(MarkerComponent.class, PlatformCompatibility.UNIVERSAL);

        ServerTree tree = ServerRenderer.render(
                Views.componentFactory(() -> new MarkerComponent(log)),
                context);

        assertEquals(new ServerElement("marker", List.of(), List.of(), List.of()),
                assertInstanceOf(ServerElement.class, tree.root()));
    }

    @Test
    void serverOnlyComponentsRender() {
        List<String> log = new ArrayList<>();
        context.compatibility().register(ThirdPartyComponent.class, PlatformCompatibility.SERVER_ONLY);

        ServerTree tree = ServerRenderer.render(
                Views.componentFactory(() -> new ThirdPartyComponent(log)),
                context);

        assertEquals(new ServerElement("third-party", List.of(), List.of(), List.of()),
                assertInstanceOf(ServerElement.class, tree.root()));
    }

    @Test
    void renderFailureStillClosesPreviouslyCreatedComponents() {
        List<String> log = new ArrayList<>();
        View inner = Views.componentFactory(() -> new InnerThrowingComponent(log));

        DejaViewException failure = assertThrows(DejaViewException.class,
                () -> ServerRenderer.render(
                        Views.componentFactory(() -> new ScriptedOuterComponent(log, inner)),
                        context));

        assertEquals("boom", failure.getMessage());
        assertEquals(
                List.of(
                        "bind:outer", "init:outer", "render:outer",
                        "bind:inner", "init:inner", "render:inner",
                        "destroy:inner", "close:inner",
                        "destroy:outer", "close:outer"),
                log);
    }

    @Test
    void nullRenderResultFailsAndComponentStillCloses() {
        List<String> log = new ArrayList<>();

        DejaViewException failure = assertThrows(DejaViewException.class,
                () -> ServerRenderer.render(
                        Views.componentFactory(() -> new NullRenderingComponent(log)),
                        context));

        assertEquals("server-render-failed", failure.code());
        assertEquals(List.of("bind:null", "init:null", "destroy:null", "close:null"), log);
    }

    @Test
    void onDestroyFailureIsReportedButCloseStillRuns() {
        List<String> log = new ArrayList<>();

        RuntimeException failure = assertThrows(RuntimeException.class,
                () -> ServerRenderer.render(
                        Views.componentFactory(() -> new DestroyThrowsComponent(log)),
                        context));

        assertEquals("destroy exploded", failure.getMessage());
        assertEquals(List.of("close:destroy-throws"), log);
    }

    @Test
    void closedContextRejectsRendering() {
        context.close();

        DejaViewException failure = assertThrows(DejaViewException.class,
                () -> ServerRenderer.render(Views.text("x"), context));

        assertEquals("closed-context", failure.code());
    }

    @Test
    void eventsNestedDeepInsideComponentsAreDroppedToo() {
        List<String> log = new ArrayList<>();

        ServerTree tree = ServerRenderer.render(
                Views.componentFactory(() -> new EventfulComponent()),
                context);

        assertEquals(List.of("submit", "click"), tree.droppedEvents());
    }

    // --- fixtures ---------------------------------------------------------

    private static final class LifecycleComponent extends AbstractComponent {
        private final String name;
        private final List<String> log;

        LifecycleComponent(String name, List<String> log) {
            this.name = name;
            this.log = log;
            onDispose(() -> log.add("close:" + name));
        }

        @Override
        public void bind(ComponentBindings bindings) {
            super.bind(bindings);
            log.add("bind:" + name);
        }

        @Override
        public void onInit() {
            log.add("init:" + name);
        }

        @Override
        public View render() {
            log.add("render:" + name);
            return element(name);
        }

        @Override
        public void onDestroy() {
            log.add("destroy:" + name);
        }

        protected View element(String tag) {
            return Views.element(tag);
        }
    }

    private static final class InputReadingComponent extends AbstractComponent {
        @Override
        public View render() {
            return Views.p(input("username", String.class));
        }
    }

    private static final class OuterComponent extends AbstractComponent {
        private final List<String> log;

        OuterComponent(List<String> log) {
            this.log = log;
            onDispose(() -> log.add("close:outer"));
        }

        @Override
        public void bind(ComponentBindings bindings) {
            super.bind(bindings);
            log.add("bind:outer");
        }

        @Override
        public void onInit() {
            log.add("init:outer");
        }

        @Override
        public View render() {
            log.add("render:outer");
            return Views.componentFactory(() -> new InnerComponent(log));
        }

        @Override
        public void onDestroy() {
            log.add("destroy:outer");
        }
    }

    private static final class InnerComponent extends AbstractComponent {
        private final List<String> log;

        InnerComponent(List<String> log) {
            this.log = log;
            onDispose(() -> log.add("close:inner"));
        }

        @Override
        public void bind(ComponentBindings bindings) {
            super.bind(bindings);
            log.add("bind:inner");
        }

        @Override
        public void onInit() {
            log.add("init:inner");
        }

        @Override
        public View render() {
            log.add("render:inner");
            return Views.text("inner");
        }

        @Override
        public void onDestroy() {
            log.add("destroy:inner");
        }
    }

    private static final class ScriptedOuterComponent extends AbstractComponent {
        private final List<String> log;
        private final View child;

        ScriptedOuterComponent(List<String> log, View child) {
            this.log = log;
            this.child = child;
            onDispose(() -> log.add("close:outer"));
        }

        @Override
        public void bind(ComponentBindings bindings) {
            super.bind(bindings);
            log.add("bind:outer");
        }

        @Override
        public void onInit() {
            log.add("init:outer");
        }

        @Override
        public View render() {
            log.add("render:outer");
            return child;
        }

        @Override
        public void onDestroy() {
            log.add("destroy:outer");
        }
    }

    private static final class InnerThrowingComponent extends AbstractComponent {
        private final List<String> log;

        InnerThrowingComponent(List<String> log) {
            this.log = log;
            onDispose(() -> log.add("close:inner"));
        }

        @Override
        public void bind(ComponentBindings bindings) {
            super.bind(bindings);
            log.add("bind:inner");
        }

        @Override
        public void onInit() {
            log.add("init:inner");
        }

        @Override
        public View render() {
            log.add("render:inner");
            throw new RuntimeException("boom");
        }

        @Override
        public void onDestroy() {
            log.add("destroy:inner");
        }
    }

    private static final class NullRenderingComponent extends AbstractComponent {
        private final List<String> log;

        NullRenderingComponent(List<String> log) {
            this.log = log;
            onDispose(() -> log.add("close:null"));
        }

        @Override
        public void bind(ComponentBindings bindings) {
            super.bind(bindings);
            log.add("bind:null");
        }

        @Override
        public void onInit() {
            log.add("init:null");
        }

        @Override
        public View render() {
            return null;
        }

        @Override
        public void onDestroy() {
            log.add("destroy:null");
        }
    }

    private static final class DestroyThrowsComponent extends AbstractComponent {
        private final List<String> log;

        DestroyThrowsComponent(List<String> log) {
            this.log = log;
            onDispose(() -> log.add("close:destroy-throws"));
        }

        @Override
        public View render() {
            return Views.text("x");
        }

        @Override
        public void onDestroy() {
            throw new RuntimeException("destroy exploded");
        }
    }

    private static final class MarkerComponent extends AbstractComponent implements BrowserOnlyComponent {
        private final List<String> log;

        MarkerComponent(List<String> log) {
            this.log = log;
            onDispose(() -> log.add("close:marker"));
        }

        @Override
        public void bind(ComponentBindings bindings) {
            super.bind(bindings);
            log.add("bind:marker");
        }

        @Override
        public View render() {
            log.add("render:marker");
            return Views.element("marker");
        }
    }

    private static final class ThirdPartyComponent extends AbstractComponent {
        private final List<String> log;

        ThirdPartyComponent(List<String> log) {
            this.log = log;
            onDispose(() -> log.add("close:third-party"));
        }

        @Override
        public void bind(ComponentBindings bindings) {
            super.bind(bindings);
            log.add("bind:third-party");
        }

        @Override
        public View render() {
            log.add("render:third-party");
            return Views.element("third-party");
        }
    }

    private static final class EventfulComponent extends AbstractComponent {
        @Override
        public View render() {
            return Views.element("form")
                    .on("submit", () -> { })
                    .addChildren(Views.button("Go").on("click", () -> { }));
        }
    }
}
