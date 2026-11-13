package io.dejaview.examples.ssr;

import io.dejaview.http.HttpHeaders;
import io.dejaview.server.ServerRequest;
import io.dejaview.server.ServerResponse;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * Serves {@link SsrApplication} with the JDK's built-in HTTP server.
 *
 * <p>The adapter is deliberately tiny: it copies the method, target and headers
 * into a transport-neutral {@link ServerRequest}, asks the application to
 * render, and writes the returned {@link ServerResponse}. Any HTTP host
 * (Jetty, Undertow, a servlet container) would do the same.</p>
 *
 * <p>Run with {@code -Dssr.port=8080} to change the port (default 8080, bound to
 * the loopback interface).</p>
 */
public final class SsrDemoMain {

    private SsrDemoMain() {
    }

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getProperty("ssr.port", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/", SsrDemoMain::handle);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        System.out.println("DejaView SSR example listening on http://127.0.0.1:" + port + "/");
    }

    private static void handle(HttpExchange exchange) throws IOException {
        ServerRequest request = new ServerRequest(
                exchange.getRequestMethod(),
                exchange.getRequestURI().toString(),
                headers(exchange),
                new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        ServerResponse response = SsrApplication.render(request).toResponse();
        for (String name : response.headers().names()) {
            exchange.getResponseHeaders().set(name, response.headers().joined(name));
        }
        byte[] body = response.body().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(response.status(), body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private static HttpHeaders headers(HttpExchange exchange) {
        HttpHeaders.Builder builder = HttpHeaders.builder();
        exchange.getRequestHeaders().forEach((name, values) -> values.forEach(value -> builder.add(name, value)));
        return builder.build();
    }
}
