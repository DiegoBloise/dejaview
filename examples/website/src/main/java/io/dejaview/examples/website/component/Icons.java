package io.dejaview.examples.website.component;

import io.dejaview.core.View;
import io.dejaview.core.Views;

/**
 * Inline stroke icons for feature cards, in the site's accent style.
 * Pure DejaView views — no external icon font or script.
 */
public final class Icons {

    private Icons() {
    }

    private static View svg(View... children) {
        return Views.element("svg", children)
                .attr("viewBox", "0 0 24 24")
                .attr("fill", "none")
                .attr("stroke", "currentColor")
                .attr("stroke-width", "1.8")
                .attr("stroke-linecap", "round")
                .attr("stroke-linejoin", "round")
                .attr("aria-hidden", "true");
    }

    private static View path(String d) {
        return Views.element("path").attr("d", d);
    }

    private static View path(String d, String opacity) {
        return Views.element("path").attr("d", d).attr("opacity", opacity);
    }

    private static View circle(String cx, String cy, String r) {
        return Views.element("circle").attr("cx", cx).attr("cy", cy).attr("r", r);
    }

    public static View code() {
        return svg(path("M8 6l-6 6 6 6M16 6l6 6-6 6"));
    }

    public static View pulse() {
        return svg(path("M2 12h4l3-9 4 18 3-9h6"));
    }

    public static View monitor() {
        return svg(path("M4 4h16v12H4z"), path("M8 20h8M12 16v4"));
    }

    public static View hexagon() {
        return svg(path("M12 2l8 4.5v9L12 20l-8-4.5v-9L12 2z"),
                path("M12 11l8-4.5M12 11v9M12 11L4 6.5"));
    }

    public static View route() {
        return svg(
                circle("6", "19", "2.5"),
                circle("18", "5", "2.5"),
                path("M8.2 17.8L15.8 6.2"),
                path("M8.5 19h7", "0.45"));
    }

    public static View download() {
        return svg(path("M12 3v13"), path("M7 11l5 5 5-5"), path("M4 21h16"));
    }

    public static View plug() {
        return svg(
                path("M9 7H6a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h9a2 2 0 0 0 2-2v-3"),
                path("M14 4h6v6"),
                path("M20 4l-9 9"));
    }

    public static View lines() {
        return svg(path("M4 5h16M4 12h10M4 19h7"), circle("19", "16", "3"));
    }

    public static View check() {
        return svg(path("M20 6L9 17l-5-5"));
    }

    public static View forms() {
        return svg(path("M5 3h14v18H5z"), path("M8 8h8M8 12h8M8 16h4"));
    }

    public static View state() {
        return svg(path("M4 6c0 1.7 3.6 3 8 3s8-1.3 8-3-3.6-3-8-3-8 1.3-8 3z"),
                path("M4 6v12c0 1.7 3.6 3 8 3s8-1.3 8-3V6"),
                path("M4 12c0 1.7 3.6 3 8 3s8-1.3 8-3", "0.45"));
    }

    public static View box() {
        return svg(path("M3 8l9-5 9 5v8l-9 5-9-5V8z"),
                path("M3 8l9 5 9-5"),
                path("M12 13v8", "0.45"));
    }

    public static View terminal() {
        return svg(path("M4 5h16v14H4z"), path("M7 10l4 3-4 3"), path("M13 16h4", "0.45"));
    }
}
