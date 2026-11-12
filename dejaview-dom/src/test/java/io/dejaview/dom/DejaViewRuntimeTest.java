package io.dejaview.dom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.dejaview.core.Views;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class DejaViewRuntimeTest {

    private final FakeDomDocument document = new FakeDomDocument();
    private final FakeDomElement container = document.createElement("main");
    private final RuntimeConfig config = RuntimeConfig.defaults();

    @Test
    void startsAndReleasesMountedView() {
        AtomicInteger clicks = new AtomicInteger();
        DejaViewRuntime runtime = DejaViewRuntime.create(document, container, config)
                .start(Views.button("Increment").onClick(clicks::incrementAndGet));

        assertTrue(runtime.isStarted());
        assertEquals(1, container.children().size());

        FakeDomElement button = assertInstanceOf(FakeDomElement.class, container.children().get(0));
        assertEquals(1, button.listenerCount("click"));

        button.fire("click");
        assertEquals(1, clicks.get());

        runtime.shutdown();

        assertTrue(runtime.isDisposed());
        assertTrue(container.children().isEmpty());
        assertEquals(0, button.listenerCount("click"));
    }

    @Test
    void shutdownIsIdempotent() {
        DejaViewRuntime runtime = DejaViewRuntime.create(document, container, config)
                .start(Views.div());

        runtime.shutdown();
        runtime.shutdown();

        assertTrue(runtime.isDisposed());
        assertTrue(container.children().isEmpty());
    }

    @Test
    void initializationHappensOnlyOnce() {
        DejaViewRuntime runtime = DejaViewRuntime.create(document, container, config)
                .start(Views.div());

        assertThrows(IllegalStateException.class, () -> runtime.start(Views.div()));
    }

    @Test
    void rejectsMountsAfterShutdown() {
        DejaViewRuntime runtime = DejaViewRuntime.create(document, container, config)
                .start(Views.div());
        runtime.shutdown();

        assertThrows(IllegalStateException.class, () -> runtime.mount(Views.div()));
        assertThrows(IllegalStateException.class, () -> runtime.start(Views.div()));
    }

    @Test
    void disposesMultipleMountsInReverseOrder() {
        DejaViewRuntime runtime = DejaViewRuntime.create(document, container, config)
                .start(Views.div());

        MountedDom first = runtime.mount(Views.span("first"));
        MountedDom second = runtime.mount(Views.span("second"));

        runtime.shutdown();

        assertTrue(first.isDisposed());
        assertTrue(second.isDisposed());
        assertTrue(container.children().isEmpty());
    }

    @Test
    void exposesRuntimeState() {
        DejaViewRuntime runtime = DejaViewRuntime.create(document, container, config);

        assertFalse(runtime.isStarted());
        assertSame(document, runtime.document());
        assertSame(container, runtime.container());
        assertSame(config, runtime.config());

        runtime.start(Views.div());

        assertTrue(runtime.isStarted());
    }

    @Test
    void usesTheConfiguredRenderer() {
        AtomicInteger renders = new AtomicInteger();
        Renderer recording = (view, domDocument, target) -> {
            renders.incrementAndGet();
            return new DomRenderer().render(view, domDocument, target);
        };

        DejaViewRuntime runtime = DejaViewRuntime
                .create(document, container, config, recording)
                .start(Views.div());

        assertEquals(1, renders.get());
        runtime.shutdown();
    }

    @Test
    void rejectsNullArguments() {
        assertThrows(NullPointerException.class,
                () -> DejaViewRuntime.create(null, container, config));
        assertThrows(NullPointerException.class,
                () -> DejaViewRuntime.create(document, null, config));
        assertThrows(NullPointerException.class,
                () -> DejaViewRuntime.create(document, container, null));
        assertThrows(NullPointerException.class,
                () -> DejaViewRuntime.create(document, container, config, null));
        assertThrows(NullPointerException.class,
                () -> DejaViewRuntime.create(document, container, config).start(null));
        assertThrows(NullPointerException.class,
                () -> DejaViewRuntime.create(document, container, config).mount(null));
    }
}
