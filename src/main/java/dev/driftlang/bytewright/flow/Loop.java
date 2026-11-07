/*******************************************************************************
 * Drift Programming Language
 * Bytewright Library
 *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)
 *
 * This source code is licensed under the Apache 2.0 License.
 * See the LICENSE file in the root directory for details.
 ******************************************************************************/

package dev.driftlang.bytewright.flow;

import dev.driftlang.bytewright.flow.exception.FlowAlreadyStartedException;
import dev.driftlang.bytewright.flow.exception.FlowNotStartedException;
import dev.driftlang.bytewright.flow.exception.NotInLoopBodyException;
import dev.driftlang.bytewright.scope.ScopeManager;
import dev.driftlang.bytewright.scope.exception.ScopeAlreadyClosedException;

import java.lang.classfile.CodeBuilder;
import java.lang.classfile.Label;


/**
 * Represents a control flow loop that contains some code to be repeated.
 * Being a minimal and shape-agnostic primitive, this class expects its usages
 * to be wrapped with appropriate actions, like a condition evaluation.
 *
 * @author Jonathan (GitHub: belicfr)
 */
public class Loop extends Flow {

    private final Label bodyLabel;
    private final Label continueLabel;

    private boolean inBody = false;


    /**
     * Creates a loop with a given parent scope.
     *
     * @param parentScope the parent instance of the current scope.
     */
    public Loop(final ScopeManager.Scope parentScope) {
        super(parentScope);

        CodeBuilder builder = getScope().getBuilder();
        this.bodyLabel = builder.newLabel();
        this.continueLabel = builder.newLabel();
    }


    /**
     * Starts this loop by binding its body label and marking it as started.
     *
     * @throws ScopeAlreadyClosedException if this flow's scope is already
     *                                     closed.
     * @throws FlowAlreadyStartedException if this flow has already been
     *                                     started.
     */
    public void start() {
        getScope().requireNotClosed();

        super.start();
        inBody = true;

        getScope().getBuilder()
                .labelBinding(bodyLabel);
    }

    /**
     * Continues this loop by generating a goto to its {@code continue} point.
     * <p>
     * The {@code continue} point is where the caller calls {@link
     * #continueEntry()}. For stepped loops ({@code for}, {@code foreach},
     * {@code do-while}), call {@link #continueEntry()} right before emitting
     * the update step or the trailing condition, so that continuing still runs
     * it.
     * <p>
     * If {@link #continueEntry()} is never called, the {@code continue} point
     * is bound when the loop closes, just before the jump back to the loop's
     * head; this is the correct place for a plain {@code while} loop.
     *
     * @throws FlowNotStartedException if this flow has not been started yet.
     * @throws ScopeAlreadyClosedException if this flow's scope is already
     *                                     closed.
     * @throws NotInLoopBodyException if the require statement has been
     *                                reached outside the loop's body, it may
     *                                occur if {@link #continueEntry()} has
     *                                already been called.
     */
    public void continue_() {
        requireActive();
        requireInBody();

        getScope().getBuilder()
                .goto_(continueLabel);
        // NOTE: as this method should be called before 'continueEntry()', the
        //  'goto continueLabel' instruction is defined before the label's
        //  binding; so CodeBuilder marks the label as placeholder/not assigned
        //  yet: the label must be bound before the structure's end!
    }

    /**
     * Exits the loop's body and binds {@link #continueLabel}. This method marks
     * the end of the loop's body and the start of the 'continue' step. {@link
     * #continue_()} will not be available afterward.
     *
     * @throws FlowNotStartedException if this flow has not been started yet.
     * @throws ScopeAlreadyClosedException if this flow's scope is already
     *                                     closed.
     */
    public void continueEntry() {
        requireActive();

        inBody = false;
        bindContinueLabel();
    }

    /**
     * Binds {@link #continueLabel} using the scope's {@link CodeBuilder}
     * instance.
     *
     * @see #continueLabel
     */
    private void bindContinueLabel() {
        getScope().getBuilder()
                .labelBinding(continueLabel);
    }

    /**
     * @throws NotInLoopBodyException if the require statement has been
     *                                reached outside the loop's body, it may
     *                                occur if {@link #continueEntry()} has
     *                                already been called.
     * @see #inBody
     */
    private void requireInBody() {
        if (!inBody)
            throw new NotInLoopBodyException();
    }


    /**
     * Binds {@link #continueLabel} if in the body context ({@link #inBody} ==
     * {@code true}); then emits a goto using {@link #bodyLabel} and finishes
     * with the flow's closing behavior.
     *
     * @throws FlowNotStartedException if the flow operation has not been
     *                                 started yet.
     * @throws ScopeAlreadyClosedException if this flow's scope is already
     *                                     closed.
     */
    @Override
    public void close() {
        requireActive();

        if (inBody)
            bindContinueLabel();

        getScope().getBuilder()
                .goto_(bodyLabel);

        super.close();
    }
}
