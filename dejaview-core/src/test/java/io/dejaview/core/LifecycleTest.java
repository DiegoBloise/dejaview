package io.dejaview.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class LifecycleTest {

    @Test
    void defaultCallbacksAreNoOps() {
        Lifecycle lifecycle = new Lifecycle() {
        };

        lifecycle.onStart();
        lifecycle.onStop();
    }

    @Test
    void implementationsCanOverrideCallbacks() {
        List<String> events = new ArrayList<>();
        Lifecycle lifecycle = new Lifecycle() {
            @Override
            public void onStart() {
                events.add("start");
            }

            @Override
            public void onStop() {
                events.add("stop");
            }
        };

        lifecycle.onStart();
        lifecycle.onStop();

        assertEquals(List.of("start", "stop"), events);
    }
}
