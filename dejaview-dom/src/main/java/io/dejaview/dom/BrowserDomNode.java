package io.dejaview.dom;

import org.teavm.jso.dom.xml.Node;

import java.util.Objects;

/**
 * Browser implementation detail: wraps a TeaVM JSO node.
 */
class BrowserDomNode implements DomNode {

    private final Node node;

    BrowserDomNode(Node node) {
        this.node = Objects.requireNonNull(node, "node");
    }

    final Node jsoNode() {
        return node;
    }
}
