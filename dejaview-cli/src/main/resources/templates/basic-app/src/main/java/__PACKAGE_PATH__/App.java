package __PACKAGE__;

import io.dejaview.core.Views;
import io.dejaview.dom.DejaView;

/**
 * Entry point of the application. Mounts {@link CounterComponent} under the
 * {@code #app} element; the DOM is managed entirely by the component tree.
 */
public final class App {

    private App() {
    }

    public static void main(String[] args) {
        DejaView.mount(Views.componentFactory(CounterComponent::new), "app");
        System.out.println("DejaView app mounted");
    }
}
