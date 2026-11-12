package io.dejaview.http;

/**
 * Credentials policy for a request.
 *
 * <p>Semantics map onto the browser transport as follows:</p>
 * <ul>
 *   <li>{@link #OMIT} — never send cookies or HTTP authentication;</li>
 *   <li>{@link #SAME_ORIGIN} — send credentials only for same-origin requests;</li>
 *   <li>{@link #INCLUDE} — send credentials for same- and cross-origin requests.</li>
 * </ul>
 *
 * <p>Implementation note: the browser transport is based on
 * {@code XMLHttpRequest}, which cannot <em>omit</em> same-origin cookies.
 * {@code OMIT} and {@code SAME_ORIGIN} both result in
 * {@code withCredentials = false}; only {@code INCLUDE} sets
 * {@code withCredentials = true}. See the HTTP client ADR (0018) for the
 * documented limitation.</p>
 */
public enum HttpCredentials {
    OMIT,
    SAME_ORIGIN,
    INCLUDE
}
