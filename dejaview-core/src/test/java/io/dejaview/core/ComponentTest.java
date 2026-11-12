package io.dejaview.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ComponentTest {

    @Test
    void defaultNameUsesTheSimpleClassName() {
        Component component = new NamedComponent();

        assertEquals("NamedComponent", component.name());
    }

    @Test
    void functionalComponentsCanBeWrittenAsLambdas() {
        Component component = () -> Views.text("content");

        assertEquals("content", ((TextView) component.render()).value());
        assertFalse(component.name().isBlank());
    }

    @Test
    void viewSupplierComponentsReportAFunctionName() {
        ComponentView view = Views.component(() -> Views.text("content"));
        Component component = view.create();

        assertEquals("Function", component.name());
    }

    @Test
    void closeIsANoOpByDefault() {
        Component component = () -> Views.text("content");

        component.close();
        component.close();
    }

    @Test
    void abstractComponentsDisposeOwnedResourcesInReverseOrder() {
        List<String> disposed = new ArrayList<>();
        OwnedComponent component = new OwnedComponent(disposed);

        component.close();

        assertEquals(List.of("second", "first"), disposed);
    }

    @Test
    void abstractComponentsRunCleanupActions() {
        AtomicInteger disposed = new AtomicInteger();
        Component component = new AbstractComponent() {
            {
                onDispose(disposed::incrementAndGet);
            }

            @Override
            public View render() {
                return Views.text("content");
            }
        };

        component.close();
        component.close();

        assertEquals(1, disposed.get());
    }

    @Test
    void abstractComponentsRejectResourcesAfterDisposal() {
        OwnedComponent component = new OwnedComponent(new ArrayList<>());
        component.close();

        assertThrows(IllegalStateException.class, () -> component.own(() -> { }));
        assertThrows(IllegalStateException.class, () -> component.onDispose(() -> { }));
    }

    @Test
    void abstractComponentsAreClosedOnlyOnce() {
        AtomicInteger disposed = new AtomicInteger();
        OwnedComponent component = new OwnedComponent(new ArrayList<>());
        component.onDispose(disposed::incrementAndGet);

        component.close();
        component.close();

        assertEquals(1, disposed.get());
        assertTrue(component.resourcesDisposed());
        assertNotNull(component.render());
    }

    private static final class NamedComponent implements Component {

        @Override
        public View render() {
            return Views.text("named");
        }
    }

    private static final class OwnedComponent extends AbstractComponent {

        private final List<String> disposed;

        private OwnedComponent(List<String> disposed) {
            this.disposed = disposed;
            own(() -> disposed.add("first"));
            own(() -> disposed.add("second"));
        }

        @Override
        public View render() {
            return Views.text("owned");
        }

        boolean resourcesDisposed() {
            return disposed.size() == 2;
        }
    }
}
