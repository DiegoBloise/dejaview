package io.dejaview.forms;

/**
 * Receives the result of an asynchronous validation.
 *
 * <p>Validators call {@link #complete(ValidationErrors)} exactly once. The
 * framework ignores stale callbacks from earlier values, so an outdated
 * response can neither overwrite newer errors nor leave a control pending.</p>
 */
@FunctionalInterface
public interface ValidationCallback {

    /**
     * Reports the validation result.
     *
     * @param errors errors found, or {@link ValidationErrors#none()}
     */
    void complete(ValidationErrors errors);
}
