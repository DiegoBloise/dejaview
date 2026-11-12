package io.dejaview.examples.website;

import io.dejaview.examples.website.bridge.PageGlobals;

/**
 * Language context for the site's two locales. The static shell pages declare
 * {@code window.DEJAVIEW_LANG}; everything renderer-side reads its copy from
 * here so EN and PT never drift apart structurally.
 */
public enum Lang {
    EN,
    PT;

    public static Lang current() {
        return "pt".equalsIgnoreCase(PageGlobals.lang()) ? PT : EN;
    }

    public boolean isPt() {
        return this == PT;
    }

    /** Picks the English or Portuguese variant of a UI string. */
    public String t(String en, String pt) {
        return isPt() ? pt : en;
    }

    public String code() {
        return isPt() ? "pt" : "en";
    }

    /** Prefix of the locale-mirrored tree: {@code ""} for EN, {@code "/pt"} for PT. */
    public String webPath() {
        return isPt() ? "/website/pt" : "/website";
    }

    public String homeUrl() {
        return webPath() + "/";
    }

    public String getStartedUrl() {
        return webPath() + "/get-started.html";
    }

    public String playgroundUrl() {
        return webPath() + "/playground/";
    }

    /** URL of the same page in the other locale (used by the language switch). */
    public String switchUrl(String page) {
        String suffix = switch (page) {
            case "home" -> "/";
            case "get-started" -> "/get-started.html";
            default -> "/playground/";
        };
        return (isPt() ? "/website" : "/website/pt") + suffix;
    }

    /** Label shown on the language switch: the other locale's name. */
    public String switchLabel() {
        return isPt() ? "EN" : "PT";
    }

    /** Long-form footer toggle label, pointing at the other locale. */
    public String switchHint() {
        return isPt() ? "View in English" : "Ver em português";
    }
}
