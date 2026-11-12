package io.dejaview.examples.website.docs;

import java.util.List;

/**
 * Content model for the user documentation pages. Pages are written as a flat
 * list of blocks; {@link BlockView} renders them. Segments give paragraphs
 * inline code, bold and links without HTML string interpolation.
 */
public final class Doc {

    private Doc() {
    }

    // ---------- Inline segments ----------

    public sealed interface Segment {
    }

    public record Text(String value) implements Segment {
    }

    public record InlineCode(String value) implements Segment {
    }

    public record Strong(String value) implements Segment {
    }

    public record Link(String href, String label) implements Segment {
    }

    public static Segment text(String value) {
        return new Text(value);
    }

    public static Segment code(String value) {
        return new InlineCode(value);
    }

    public static Segment strong(String value) {
        return new Strong(value);
    }

    public static Segment link(String href, String label) {
        return new Link(href, label);
    }

    public static List<Segment> segments(Segment... parts) {
        return List.of(parts);
    }

    // ---------- Blocks ----------

    public sealed interface Block {
    }

    public record H2(String id, String text) implements Block {
    }

    public record H3(String text) implements Block {
    }

    public record Prose(List<Segment> segments) implements Block {
        public Prose {
            segments = List.copyOf(segments);
        }
    }

    public record FencedCode(String source, boolean highlightJava) implements Block {
    }

    public record Bullets(List<List<Segment>> items, boolean ordered) implements Block {
        public Bullets {
            items = items.stream().map(List::copyOf).toList();
        }
    }

    /** Callout box; tone is one of {@code info}, {@code tip}, {@code warning}. */
    public record Note(String tone, List<Segment> segments) implements Block {
        public Note {
            segments = List.copyOf(segments);
        }
    }

    public static Block h2(String id, String text) {
        return new H2(id, text);
    }

    public static Block h3(String text) {
        return new H3(text);
    }

    public static Block prose(Segment... parts) {
        return new Prose(segments(parts));
    }

    public static Block java(String source) {
        return new FencedCode(source, true);
    }

    public static Block shell(String source) {
        return new FencedCode(source, false);
    }

    /** One bullet item with mixed inline segments. */
    public static List<Segment> item(Segment... parts) {
        return List.of(parts);
    }

    @SafeVarargs
    public static Block bullets(List<Segment>... items) {
        return new Bullets(List.of(items), false);
    }

    @SafeVarargs
    public static Block ordered(List<Segment>... items) {
        return new Bullets(List.of(items), true);
    }

    public static Block note(String tone, Segment... parts) {
        return new Note(tone, segments(parts));
    }
}
