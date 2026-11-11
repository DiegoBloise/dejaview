package io.dejaview.reactivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the observers interested in a reactive value.
 *
 * <p>Both {@link Signal} and {@link Computed} own one of these. Keeping the
 * observer list in a separate type avoids exposing observer management in the
 * public API and lets computeds depend on other computeds.</p>
 */
final class ReactiveSource {

    private final List<ReactiveObserver> observers = new ArrayList<>();

    void addObserver(ReactiveObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    void removeObserver(ReactiveObserver observer) {
        observers.remove(observer);
    }

    void notifyObservers() {
        // Copy so an observer can dispose itself or another observer while
        // running without breaking the iteration.
        for (ReactiveObserver observer : new ArrayList<>(observers)) {
            observer.invalidate();
        }
    }
}
