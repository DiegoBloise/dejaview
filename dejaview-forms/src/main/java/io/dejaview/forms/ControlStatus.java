package io.dejaview.forms;

/**
 * Lifecycle state of a form control.
 *
 * <p>The state is derived from the control's own validators, its descendants
 * and whether it is disabled. It is exposed as a value rather than as a set of
 * independent booleans so applications can render a single status.</p>
 */
public enum ControlStatus {

    /** The control and every descendant are valid. */
    VALID,

    /** The control or at least one descendant has validation errors. */
    INVALID,

    /** An asynchronous validation is in flight. */
    PENDING,

    /** The control (or one of its ancestors) is disabled. */
    DISABLED
}
