package io.dejaview.examples.ssg;

import io.dejaview.core.AbstractComponent;
import io.dejaview.core.View;
import io.dejaview.core.Views;
import io.dejaview.ssg.SsgRoute;
import io.dejaview.ssg.SsgSite;
import io.dejaview.ssr.PageMeta;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * A small static site built with DejaView components.
 *
 * <p>Every page is an ordinary component: it writes {@link PageMeta} while it
 * renders and the generator turns the resulting tree into a file. The routes
 * are declared explicitly — no reflection, no annotation scanning — and the
 * product pages are expanded from the catalogue at build time.</p>
 *
 * <p>The generated files are plain HTML plus one CSS file: upload
 * {@code target/site} to any static host (GitHub Pages, S3, Netlify, nginx) and
 * the site works with JavaScript disabled.</p>
 */
public final class StaticSite {

    static final List<Product> CATALOGUE = List.of(
            new Product("mechanical-keyboard", "Mechanical keyboard", "Hot-swappable switches and a compact layout."),
            new Product("wireless-mouse", "Wireless mouse", "Silent clicks and a two-month battery."),
            new Product("desk-shelf", "Desk & Shelf", "Solid oak, assembled without tools."));

    private StaticSite() {
    }

    /**
     * Describes the site.
     *
     * @param outputDir the directory that will hold the deployable site
     * @param assetsDir the directory copied to {@code /assets}
     */
    public static SsgSite site(Path outputDir, Path assetsDir) {
        return SsgSite.builder(outputDir)
                .baseUrl("https://dejaview.example")
                .route(SsgRoute.page("/", "en", meta -> Views.componentFactory(() -> new HomePage(meta))))
                .route(SsgRoute.page("/about", "en", meta -> Views.componentFactory(() -> new AboutPage(meta))))
                .route(SsgRoute.page("/products", "en", meta -> Views.componentFactory(() -> new ProductsPage(meta))))
                .route(SsgRoute.parametrized(
                        "/products/{slug}", "en",
                        CATALOGUE.stream().map(product -> Map.of("slug", product.slug())).toList(),
                        ctx -> Views.componentFactory(() -> new ProductPage(ctx.meta(), product(ctx.param("slug"))))))
                .route(SsgRoute.page("/404.html", "en", meta -> Views.componentFactory(() -> new NotFoundPage(meta))))
                .asset(assetsDir, "assets")
                .build();
    }

    static Product product(String slug) {
        return CATALOGUE.stream()
                .filter(candidate -> candidate.slug().equals(slug))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown product: " + slug));
    }

    static View link(String href, String label) {
        return Views.element("a", Views.text(label)).attr("href", href);
    }

    static View page(View... children) {
        return Views.div()
                .attr("class", "page")
                .addChildren(Views.element("link").attr("rel", "stylesheet").attr("href", "/assets/site.css"))
                .addChildren(children);
    }

    static View navigation() {
        return Views.element("nav",
                        link("/", "Home"),
                        link("/products", "Products"),
                        link("/about", "About"))
                .attr("aria-label", "Main navigation");
    }

    static View productList() {
        List<View> items = CATALOGUE.stream()
                .map(product -> (View) Views.li(link("/products/" + product.slug(), product.name())))
                .toList();
        return Views.ul(items.toArray(View[]::new));
    }

    /** Landing page. */
    static final class HomePage extends AbstractComponent {
        private final PageMeta meta;

        HomePage(PageMeta meta) {
            this.meta = meta;
        }

        @Override
        public View render() {
            meta.title("DejaView static site")
                    .description("A statically generated site built from DejaView components.")
                    .metaProperty("og:type", "website");
            return page(
                    navigation(),
                    Views.h1("DejaView static site"),
                    Views.p("Every page in this directory was rendered by Java at build time "
                            + "and written as plain HTML."),
                    Views.p(link("/products", "Browse the catalogue")),
                    Views.p(link("/about", "How it is built")));
        }
    }

    /** About page. */
    static final class AboutPage extends AbstractComponent {
        private final PageMeta meta;

        AboutPage(PageMeta meta) {
            this.meta = meta;
        }

        @Override
        public View render() {
            meta.title("About")
                    .description("How this DejaView site is generated.");
            return page(
                    navigation(),
                    Views.h1("About"),
                    Views.p("The build declares its routes explicitly, renders each one "
                            + "through dejaview-ssr and writes index.html files with copied assets. "
                            + "There is no server runtime and no client JavaScript."),
                    Views.p(link("/", "Back to home")));
        }
    }

    /** Catalogue index. */
    static final class ProductsPage extends AbstractComponent {
        private final PageMeta meta;

        ProductsPage(PageMeta meta) {
            this.meta = meta;
        }

        @Override
        public View render() {
            meta.title("Products")
                    .description("Every product in the static catalogue.");
            return page(
                    navigation(),
                    Views.h1("Products"),
                    productList());
        }
    }

    /** One catalogue entry, generated per parameter set. */
    static final class ProductPage extends AbstractComponent {
        private final PageMeta meta;
        private final Product product;

        ProductPage(PageMeta meta, Product product) {
            this.meta = meta;
            this.product = product;
        }

        @Override
        public View render() {
            meta.title(product.name())
                    .description(product.description());
            return page(
                    navigation(),
                    Views.h1(product.name()),
                    Views.p(product.description()),
                    Views.p(link("/products", "All products")));
        }
    }

    /** Static 404 document; static hosts can serve it as their error page. */
    static final class NotFoundPage extends AbstractComponent {
        private final PageMeta meta;

        NotFoundPage(PageMeta meta) {
            this.meta = meta;
        }

        @Override
        public View render() {
            meta.title("Not found").description("The requested page does not exist.");
            return page(
                    navigation(),
                    Views.h1("Not found"),
                    Views.p("The requested page does not exist."),
                    Views.p(link("/", "Back to home")));
        }
    }

    /** One catalogue entry. */
    record Product(String slug, String name, String description) {
    }
}
