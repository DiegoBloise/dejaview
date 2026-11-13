package io.dejaview.examples.ssr;

import io.dejaview.core.AbstractComponent;
import io.dejaview.core.View;
import io.dejaview.core.Views;
import io.dejaview.di.Injector;
import io.dejaview.server.ServerContext;
import io.dejaview.server.ServerErrorBoundary;
import io.dejaview.server.ServerRequest;
import io.dejaview.ssr.PageMeta;
import io.dejaview.ssr.SsrRenderer;
import io.dejaview.ssr.SsrResult;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The demo application: one request in, one complete HTML page out.
 *
 * <p>Every render creates a fresh root injector and a request-scoped
 * {@link ServerContext}, so nothing survives between requests except the
 * immutable catalogue. The pages are ordinary DejaView components rendered by
 * {@link SsrRenderer}; links and the search form are plain HTML and work with
 * JavaScript disabled.</p>
 *
 * <p>Routes:</p>
 * <ul>
 *   <li>{@code /} — landing page;</li>
 *   <li>{@code /products?q=} — catalogue filtered by a case-insensitive
 *       substring;</li>
 *   <li>{@code /products?fail=1} — the same page with a failing price widget,
 *       contained by a {@link ServerErrorBoundary};</li>
 *   <li>anything else — a 404 page.</li>
 * </ul>
 */
public final class SsrApplication {

    private static final List<Product> CATALOGUE = List.of(
            new Product("Mechanical keyboard", 8900),
            new Product("Wireless mouse", 3900),
            new Product("27-inch monitor", 31900),
            new Product("USB-C hub", 5900),
            new Product("Laptop stand", 4500));

    private SsrApplication() {
    }

    /**
     * Renders the page for {@code request}.
     *
     * @param request the request to answer; never {@code null}
     * @return the render result, including the HTTP status the host should send
     */
    public static SsrResult render(ServerRequest request) {
        PageMeta meta = new PageMeta().lang("en");
        String path = pathOf(request.path());
        boolean found = true;
        View view;
        switch (path) {
            case "/" -> view = Views.componentFactory(() -> new HomePage(meta));
            case "/products" -> view = Views.componentFactory(() -> new ProductsPage(
                    meta,
                    queryParam(request, "q"),
                    "1".equals(queryParam(request, "fail"))));
            default -> {
                found = false;
                view = Views.componentFactory(() -> new NotFoundPage(meta));
            }
        }
        Injector root = Injector.builder().name("ssr-example").build();
        try (ServerContext context = ServerContext.create(root, request)) {
            SsrResult result = SsrRenderer.renderPage(view, context, meta);
            return found || !result.isSuccess() ? result : withStatus(result, 404);
        } finally {
            root.close();
        }
    }

    private static SsrResult withStatus(SsrResult result, int status) {
        return new SsrResult(
                status,
                result.html(),
                result.droppedEvents(),
                result.boundaryErrors(),
                result.warnings(),
                result.error());
    }

    static String pathOf(String requestTarget) {
        int query = requestTarget.indexOf('?');
        String path = query < 0 ? requestTarget : requestTarget.substring(0, query);
        return path.isEmpty() ? "/" : path;
    }

    static String queryParam(ServerRequest request, String name) {
        String target = request.path();
        int query = target.indexOf('?');
        if (query < 0) {
            return "";
        }
        for (String pair : target.substring(query + 1).split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int equals = pair.indexOf('=');
            String key = equals < 0 ? pair : pair.substring(0, equals);
            if (!key.equals(name)) {
                continue;
            }
            String raw = equals < 0 ? "" : pair.substring(equals + 1);
            try {
                return URLDecoder.decode(raw, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException malformed) {
                return raw;
            }
        }
        return "";
    }

    static List<Product> search(String query) {
        if (query == null || query.isBlank()) {
            return CATALOGUE;
        }
        String needle = query.toLowerCase(Locale.ROOT);
        List<Product> matches = new ArrayList<>();
        for (Product product : CATALOGUE) {
            if (product.name().toLowerCase(Locale.ROOT).contains(needle)) {
                matches.add(product);
            }
        }
        return List.copyOf(matches);
    }

    private static View link(String href, String label) {
        return Views.element("a", Views.text(label)).attr("href", href);
    }

    private static View page(View... children) {
        return Views.div().attr("class", "page").addChildren(children);
    }

    // --- pages ------------------------------------------------------------

    /**
     * Landing page. Writes its metadata while rendering, so the host only
     * creates the mutable {@link PageMeta} and the pipeline assembles the head
     * afterwards.
     */
    static final class HomePage extends AbstractComponent {
        private final PageMeta meta;

        HomePage(PageMeta meta) {
            this.meta = meta;
        }

        @Override
        public View render() {
            meta.title("DejaView SSR")
                    .description("A DejaView application rendered to HTML on the server, without JavaScript.")
                    .metaProperty("og:type", "website");
            return page(
                    Views.h1("DejaView SSR"),
                    Views.p("This page is HTML produced by Java on the server. "
                            + "It works with JavaScript disabled."),
                    Views.p(link("/products", "Browse products")),
                    Views.p(link("/products?fail=1", "See a contained failure")));
        }
    }

    /**
     * Catalogue page. The search query is echoed in the heading and in the
     * result summary; the serializer escapes it, so markup in {@code q} stays
     * text.
     */
    static final class ProductsPage extends AbstractComponent {
        private final PageMeta meta;
        private final String query;
        private final boolean failPrices;

        ProductsPage(PageMeta meta, String query, boolean failPrices) {
            this.meta = meta;
            this.query = query;
            this.failPrices = failPrices;
        }

        @Override
        public View render() {
            List<Product> matches = search(query);
            String heading = query.isEmpty()
                    ? "Products"
                    : "Products matching \"" + query + "\"";
            meta.title(heading)
                    .description(matches.size() + " product(s) match the search.")
                    .metaProperty("og:type", "website");
            List<View> items = new ArrayList<>();
            for (Product product : matches) {
                items.add(Views.li(product.name() + " — " + product.price()));
            }
            return page(
                    Views.h1(heading),
                    searchForm(),
                    Views.p(matches.isEmpty()
                            ? "Nothing matches that search. Try \"key\" or \"mouse\"."
                            : matches.size() + " product(s) found."),
                    matches.isEmpty() ? Views.fragment() : Views.ul(items.toArray(View[]::new)),
                    Views.componentFactory(SectionBoundary::new)
                            .slot(Views.componentFactory(() -> new PriceSummary(failPrices))),
                    Views.p(link("/", "Back to home")));
        }

        private View searchForm() {
            return Views.element("form")
                    .attr("action", "/products")
                    .attr("method", "get")
                    .addChildren(
                            Views.element("label").attr("for", "q").child(Views.text("Search")),
                            Views.element("input")
                                    .attr("id", "q")
                                    .attr("name", "q")
                                    .attr("type", "text")
                                    .attr("value", query),
                            Views.button("Search"));
        }
    }

    /**
     * A failing widget used to demonstrate error boundaries. The server render
     * catches its failure and keeps the rest of the page.
     */
    static final class PriceSummary extends AbstractComponent {
        private final boolean fail;

        PriceSummary(boolean fail) {
            this.fail = fail;
        }

        @Override
        public View render() {
            if (fail) {
                throw new IllegalStateException("price service unavailable");
            }
            int total = 0;
            for (Product product : CATALOGUE) {
                total += product.priceCents();
            }
            return Views.p("Catalogue total: " + Product.formatPrice(total));
        }
    }

    /**
     * Replaces a failed subtree with a readable message instead of failing the
     * whole request.
     */
    static final class SectionBoundary extends AbstractComponent implements ServerErrorBoundary {
        @Override
        public View render() {
            return Views.fragment(slot().toArray(View[]::new));
        }

        @Override
        public View fallback(Throwable error) {
            return Views.p("Prices are temporarily unavailable.");
        }
    }

    /**
     * Rendered for unknown paths; the host maps it to status 404.
     */
    static final class NotFoundPage extends AbstractComponent {
        private final PageMeta meta;

        NotFoundPage(PageMeta meta) {
            this.meta = meta;
        }

        @Override
        public View render() {
            meta.title("Not found").description("The requested page does not exist.");
            return page(
                    Views.h1("Not found"),
                    Views.p("The requested page does not exist."),
                    Views.p(link("/", "Back to home")));
        }
    }
}
