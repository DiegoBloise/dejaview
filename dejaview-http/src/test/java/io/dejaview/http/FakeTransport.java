package io.dejaview.http;

import java.util.function.Consumer;

/**
 * A controllable in-memory transport for tests. Either responds synchronously
 * (when {@code respondInline} is set) or records the handler so the test can
 * settle the call manually.
 */
class FakeTransport implements HttpTransport {

    HttpRequest request;
    Consumer<HttpResult> handler;
    HttpResult inlineResult;
    boolean respondInline = true;

    final FakeCall call = new FakeCall();

    FakeTransport respondingWith(HttpResult result) {
        this.inlineResult = result;
        this.respondInline = true;
        return this;
    }

    FakeTransport manual() {
        this.respondInline = false;
        return this;
    }

    void deliver(HttpResult result) {
        handler.accept(result);
    }

    @Override
    public HttpCall execute(HttpRequest request, Consumer<HttpResult> handler) {
        this.request = request;
        this.handler = handler;
        if (respondInline) {
            handler.accept(inlineResult);
        }
        return call;
    }

    static final class FakeCall implements HttpCall {
        boolean cancelled;
        boolean done;

        @Override
        public void cancel() {
            cancelled = true;
        }

        @Override
        public boolean isDone() {
            return done;
        }
    }
}
