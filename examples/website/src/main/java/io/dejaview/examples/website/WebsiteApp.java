package io.dejaview.examples.website;

import io.dejaview.core.Views;
import io.dejaview.dom.BrowserEnvironment;
import io.dejaview.dom.DejaView;
import io.dejaview.examples.website.bridge.PageGlobals;
import io.dejaview.examples.website.docs.GetStartedPage;
import io.dejaview.examples.website.home.HomePage;
import io.dejaview.examples.website.playground.EditorWidget;
import io.dejaview.examples.website.playground.PlaygroundPage;
import io.dejaview.examples.website.bridge.Highlighter;

/**
 * Entry point for the DejaView website bundle.
 *
 * <p>The site itself is a DejaView application. Static shells in
 * {@code website/} declare {@code window.DEJAVIEW_PAGE} ({@code "home"},
 * {@code "get-started"} or {@code "playground"}) and {@code window.DEJAVIEW_LANG}
 * ({@code "en"} or {@code "pt"}), then load this bundle and call
 * {@code main()}. Everything on screen afterwards is rendered by the framework.
 */
public final class WebsiteApp {

    private WebsiteApp() {
    }

    public static void main(String[] args) {
        if (!BrowserEnvironment.isBrowser()) {
            return;
        }
        Lang lang = Lang.current();
        String page = PageGlobals.page();
        DejaView.mount(Views.componentFactory(factory(lang, page)), "app");
        Highlighter.highlightAll();
        EditorWidget.init();
    }

    private static java.util.function.Supplier<io.dejaview.core.Component> factory(Lang lang, String page) {
        return switch (page) {
            case "get-started" -> () -> new GetStartedPage(lang);
            case "playground" -> () -> new PlaygroundPage(lang);
            default -> () -> new HomePage(lang);
        };
    }
}
