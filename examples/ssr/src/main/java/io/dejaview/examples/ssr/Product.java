package io.dejaview.examples.ssr;

/**
 * One catalogue entry rendered by the SSR example.
 *
 * @param name       the display name, never blank
 * @param priceCents the price in cents
 */
public record Product(String name, int priceCents) {

    /**
     * Formats cents as a plain decimal amount, for example {@code 8900} becomes
     * {@code "89.00"}.
     */
    public static String formatPrice(int priceCents) {
        return (priceCents / 100) + "." + String.format(java.util.Locale.ROOT, "%02d", priceCents % 100);
    }

    /**
     * Returns the price formatted for display.
     */
    public String price() {
        return formatPrice(priceCents);
    }
}
