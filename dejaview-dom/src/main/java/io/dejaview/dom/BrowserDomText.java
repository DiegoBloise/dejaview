package io.dejaview.dom;

import org.teavm.jso.dom.xml.Node;

import java.util.Objects;

/**
 * Browser implementation detail: wraps a TeaVM JSO text node.
 */
final class BrowserDomText extends BrowserDomNode implements DomText {

    BrowserDomText(Node node) {
        super(node);
    }

    @Override
    public void setText(String text) {
        jsoNode().setTextContent(Objects.requireNonNull(text, "text"));
    }
}
