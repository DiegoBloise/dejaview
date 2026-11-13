package io.dejaview.server;

/**
 * Marker interface for components that require browser APIs.
 *
 * <p>A component implementing this interface — or registered as
 * {@link PlatformCompatibility#BROWSER_ONLY} in a
 * {@link PlatformCompatibilityRegistry} — is rejected by the server renderer
 * with a {@code browser-only-component} error instead of being constructed
 * blindly on the server, where {@code window}, {@code document} and browser
 * events do not exist.</p>
 *
 * <p>Prefer this marker for application code. The registry exists for
 * third-party components you cannot modify.</p>
 */
public interface BrowserOnlyComponent {
}
