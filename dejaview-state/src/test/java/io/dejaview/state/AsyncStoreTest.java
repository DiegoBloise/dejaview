package io.dejaview.state;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncStoreTest {

    /** A loader whose callbacks are completed manually by the test. */
    private static final class ControllableLoader implements AsyncLoader<String> {

        Consumer<String> succeed;
        Consumer<AsyncError> fail;
        int attempts;

        @Override
        public void load(Consumer<String> succeed, Consumer<AsyncError> fail) {
            this.attempts++;
            this.succeed = succeed;
            this.fail = fail;
        }
    }

    @Test
    void startsIdleAndExposesLoadingThenSuccess() {
        ControllableLoader loader = new ControllableLoader();
        AsyncStore<String> store = new AsyncStore<>(loader);

        assertEquals(AsyncStatus.IDLE, store.state().status());
        assertTrue(store.state().isIdle());
        assertTrue(store.state().data().isEmpty());

        store.load();
        assertEquals(AsyncStatus.LOADING, store.state().status());
        assertTrue(store.isLoading());
        assertEquals(1, loader.attempts);

        loader.succeed.accept("ready");
        assertEquals(AsyncStatus.SUCCESS, store.state().status());
        assertEquals(Optional.of("ready"), store.state().data());
        assertTrue(store.state().error().isEmpty());
        assertFalse(store.isLoading());
    }

    @Test
    void failureExposesStructuredErrorAndKeepsPreviousData() {
        ControllableLoader loader = new ControllableLoader();
        AsyncStore<String> store = new AsyncStore<>(loader);

        store.load();
        loader.succeed.accept("first");

        store.reload();
        assertEquals(Optional.of("first"), store.state().data());

        loader.fail.accept(AsyncError.of("offline", "no route to the network"));
        assertEquals(AsyncStatus.FAILURE, store.state().status());
        assertEquals(Optional.of("first"), store.state().data());
        assertEquals(Optional.of("offline"), store.state().error().map(AsyncError::code));
    }

    @Test
    void supersededAttemptsAreDiscarded() {
        ControllableLoader loader = new ControllableLoader();
        AsyncStore<String> store = new AsyncStore<>(loader);

        store.load();                       // attempt 1
        Consumer<String> staleSucceed = loader.succeed;
        Consumer<AsyncError> staleFail = loader.fail;
        store.load();                       // attempt 2 supersedes attempt 1

        staleSucceed.accept("stale value");
        assertEquals(AsyncStatus.LOADING, store.state().status());

        loader.succeed.accept("fresh value");
        assertEquals(Optional.of("fresh value"), store.state().data());

        staleFail.accept(AsyncError.of("late", "too late"));
        assertEquals(AsyncStatus.SUCCESS, store.state().status());
        assertTrue(store.state().error().isEmpty());
    }

    @Test
    void failingFastFailuresDoNotCorruptState() {
        AsyncStore<String> store = new AsyncStore<>((succeed, fail) -> {
            throw new IllegalStateException("boom");
        });

        store.load();
        assertEquals(AsyncStatus.FAILURE, store.state().status());
        assertEquals(Optional.of("load-error"), store.state().error().map(AsyncError::code));
    }

    @Test
    void exactlyOneOutcomeWinsPerAttempt() {
        AsyncStore<String> store = new AsyncStore<>((succeed, fail) -> {
            succeed.accept("first");
            fail.accept(AsyncError.of("second", "arrived second"));
        });

        store.load();
        assertEquals(AsyncStatus.SUCCESS, store.state().status());
        assertEquals(Optional.of("first"), store.state().data());
    }

    @Test
    void synchronousLoaderCompletesInline() {
        AsyncStore<String> store = new AsyncStore<>((succeed, fail) -> succeed.accept("done"));
        store.load();
        assertEquals(AsyncStatus.SUCCESS, store.state().status());
        assertEquals(Optional.of("done"), store.state().data());
    }

    @Test
    void disposeMidFlightDropsCompletionsAndRejectsFurtherLoads() {
        ControllableLoader loader = new ControllableLoader();
        AsyncStore<String> store = new AsyncStore<>(loader);

        store.load();
        store.dispose();
        assertTrue(store.isDisposed());

        loader.succeed.accept("ignored"); // must be a silent no-op
        assertThrows(IllegalStateException.class, store::load);
        assertThrows(IllegalStateException.class, store::state);
    }
}
