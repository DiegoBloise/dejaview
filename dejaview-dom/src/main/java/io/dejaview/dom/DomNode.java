package io.dejaview.dom;

/**
 * A node in a platform DOM.
 *
 * <p>This is a deliberately small abstraction, not a replacement for the
 * browser DOM. It exists so the renderer can be exercised on the JVM with a
 * fake implementation while the browser implementation remains a thin adapter
 * over TeaVM's JSO APIs.</p>
 */
public interface DomNode {
}
