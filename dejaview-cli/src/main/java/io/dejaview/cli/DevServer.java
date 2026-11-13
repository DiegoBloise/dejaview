package io.dejaview.cli;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Local development server. Serves the static TeaVM output from {@code target/webapp},
 * injects a tiny live-reload client into HTML responses and exposes a monotonically
 * increasing build version at {@code /__dejaview__/version}. The client polls the
 * version every second and reloads the page when it changes.
 *
 * <p>The server holds no global state: the watching build calls {@link #bumpVersion()}
 * and the next poll picks it up. All responses are {@code no-store}; this is a
 * development server, not a production file server.</p>
 */
public final class DevServer implements AutoCloseable {

    static final String VERSION_PATH = "/__dejaview__/version";
    static final String LIVE_JS_PATH = "/__dejaview__/live.js";
    static final String LIVE_JS_SNIPPET = "<script src=\"/__dejaview__/live.js\" defer></script>";

    private static final String LIVE_JS = """
            (function () {
              var current = null;
              function poll() {
                fetch('/__dejaview__/version', { cache: 'no-store' })
                  .then(function (r) { return r.text(); })
                  .then(function (v) {
                    if (current === null) { current = v; return; }
                    if (v !== current) { current = v; window.location.reload(); }
                  })
                  .catch(function () { /* server restarting; poll again next tick */ });
              }
              setInterval(poll, 1000);
              poll();
            })();
            """;

    private static final Map<String, String> CONTENT_TYPES = contentTypes();

    private final HttpServer server;
    private final Path webRoot;
    private volatile long version;

    private DevServer(HttpServer server, Path webRoot) {
        this.server = server;
        this.webRoot = webRoot;
    }

    public static DevServer create(String host, int port, Path webRoot) throws IOException {
        Path normalizedRoot = webRoot.toAbsolutePath().normalize();
        HttpServer http = HttpServer.create(new InetSocketAddress(host, port), 64);
        DevServer dev = new DevServer(http, normalizedRoot);
        http.createContext(VERSION_PATH, dev::handleVersion);
        http.createContext(LIVE_JS_PATH, dev::handleLiveJs);
        http.createContext("/", dev::handleStatic);
        http.setExecutor(Executors.newFixedThreadPool(4));
        return dev;
    }

    public void start() {
        server.start();
    }

    public int port() {
        return server.getAddress().getPort();
    }

    public long version() {
        return version;
    }

    public void bumpVersion() {
        version++;
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void handleVersion(HttpExchange exchange) throws IOException {
        if (!isGet(exchange)) {
            sendText(exchange, 405, "Method Not Allowed");
            return;
        }
        byte[] body = Long.toString(version).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        sendBytes(exchange, 200, body);
    }

    private void handleLiveJs(HttpExchange exchange) throws IOException {
        if (!isGet(exchange)) {
            sendText(exchange, 405, "Method Not Allowed");
            return;
        }
        exchange.getResponseHeaders().set("Content-Type", "application/javascript; charset=utf-8");
        sendBytes(exchange, 200, LIVE_JS.getBytes(StandardCharsets.UTF_8));
    }

    private void handleStatic(HttpExchange exchange) throws IOException {
        if (!isGet(exchange) && !isHead(exchange)) {
            sendText(exchange, 405, "Method Not Allowed");
            return;
        }
        String rawPath = exchange.getRequestURI().getPath();
        String relative = rawPath == null ? "" : rawPath;
        if (relative.endsWith("/")) {
            relative = relative + "index.html";
        }
        if (relative.startsWith("/")) {
            relative = relative.substring(1);
        }
        Path resolved = webRoot.resolve(relative).normalize();
        if (!resolved.startsWith(webRoot)) {
            sendText(exchange, 403, "Forbidden: path escapes the web root.");
            return;
        }
        if (!Files.isRegularFile(resolved)) {
            sendText(exchange, 404, "Not found: " + rawPath);
            return;
        }
        byte[] content = Files.readAllBytes(resolved);
        String fileName = resolved.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        boolean isHtml = fileName.endsWith(".html") || fileName.endsWith(".htm");
        if (isHtml) {
            content = injectLiveReload(content);
        }
        exchange.getResponseHeaders().set("Content-Type", contentType(fileName));
        sendBytes(exchange, 200, content);
    }

    private static boolean isGet(HttpExchange exchange) {
        return "GET".equalsIgnoreCase(exchange.getRequestMethod());
    }

    private static boolean isHead(HttpExchange exchange) {
        return "HEAD".equalsIgnoreCase(exchange.getRequestMethod());
    }

    private static byte[] injectLiveReload(byte[] html) {
        String text = new String(html, StandardCharsets.UTF_8);
        if (text.contains(LIVE_JS_PATH)) {
            return html;
        }
        int idx = indexOfIgnoreCase(text, "</body>");
        if (idx < 0) {
            return (text + LIVE_JS_SNIPPET).getBytes(StandardCharsets.UTF_8);
        }
        return (text.substring(0, idx) + LIVE_JS_SNIPPET + text.substring(idx)).getBytes(StandardCharsets.UTF_8);
    }

    private static int indexOfIgnoreCase(String haystack, String needle) {
        String lower = haystack.toLowerCase(java.util.Locale.ROOT);
        return lower.lastIndexOf(needle.toLowerCase(java.util.Locale.ROOT));
    }

    private static String contentType(String fileName) {
        if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
            return "text/html; charset=utf-8";
        }
        int dot = fileName.lastIndexOf('.');
        String ext = dot >= 0 ? fileName.substring(dot + 1) : "";
        return CONTENT_TYPES.getOrDefault(ext, "application/octet-stream");
    }

    private static Map<String, String> contentTypes() {
        Map<String, String> types = new HashMap<>();
        types.put("js", "application/javascript; charset=utf-8");
        types.put("mjs", "application/javascript; charset=utf-8");
        types.put("css", "text/css; charset=utf-8");
        types.put("map", "application/json; charset=utf-8");
        types.put("json", "application/json; charset=utf-8");
        types.put("png", "image/png");
        types.put("jpg", "image/jpeg");
        types.put("jpeg", "image/jpeg");
        types.put("gif", "image/gif");
        types.put("svg", "image/svg+xml");
        types.put("ico", "image/x-icon");
        types.put("webp", "image/webp");
        types.put("woff", "font/woff");
        types.put("woff2", "font/woff2");
        types.put("ttf", "font/ttf");
        types.put("txt", "text/plain; charset=utf-8");
        types.put("wasm", "application/wasm");
        types.put("pdf", "application/pdf");
        types.put("webmanifest", "application/manifest+json");
        return Map.copyOf(types);
    }

    private void sendText(HttpExchange exchange, int status, String body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        sendBytes(exchange, status, body.getBytes(StandardCharsets.UTF_8));
    }

    private void sendBytes(HttpExchange exchange, int status, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        if (isHead(exchange)) {
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
            return;
        }
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.getResponseBody().close();
        exchange.close();
    }
}
