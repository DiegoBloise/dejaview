package io.dejaview.examples.counter;

import io.dejaview.core.Views;
import io.dejaview.dom.DejaView;

/**
 * Entry point of the example application.
 *
 * <p>Mounts {@link CounterComponent} through the component factory API: the
 * renderer instantiates one component per mounted node and owns its lifecycle.
 * The application itself holds no state and never touches the DOM directly.</p>
 */
public final class CounterApp {

    private CounterApp() {
    }

    public static void main(String[] args) {
        DejaView.mount(Views.componentFactory(CounterComponent::new), "app");
        System.out.println("DejaView counter mounted");
    }
}
