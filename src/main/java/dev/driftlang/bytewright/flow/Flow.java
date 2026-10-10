/*******************************************************************************
 * Drift Programming Language
 * ByteWright Library
 *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)
 *
 * This source code is licensed under the Apache 2.0 License.
 * See the LICENSE file in the root directory for details.
 ******************************************************************************/

package dev.driftlang.bytewright.flow;

import dev.driftlang.bytewright.flow.exception.FlowAlreadyStartedException;
import dev.driftlang.bytewright.flow.exception.FlowNotStartedException;
import dev.driftlang.bytewright.scope.ScopeManager;
import dev.driftlang.bytewright.scope.exception.ScopeAlreadyClosedException;
import lombok.Getter;

import java.lang.classfile.Label;


/**
 * Base class for control flows. All its implementations share a common
 * lifecycle: each opens its own sub-scope and closes it when done and gives an
 * end label to go to for leaving the scope.
 *
 * @author Jonathan (GitHub: belicfr)
 */
public abstract class Flow implements AutoCloseable {

    @Getter
    protected final ScopeManager.Scope scope;

    protected final Label endLabel;

    @Getter
    private boolean started = false;


    protected Flow(final ScopeManager.Scope parentScope) {
        this.scope = parentScope.scope();
        this.endLabel = getScope().getBuilder().newLabel();
    }


    /**
     * Starts the flow operation. Can only be called once.
     *
     * @throws FlowAlreadyStartedException if this flow has already been
     *                                     started.
     */
    protected void start() {
        requireNotStarted();

        started = true;
    }

    /**
     * Breaks this flow by generating a goto to its end label.
     *
     * @throws FlowNotStartedException if this flow has not been started yet.
     * @throws ScopeAlreadyClosedException if this flow's scope is already
     *                                     closed.
     */
    public void break_() {
        requireActive();

        getScope().getBuilder()
                .goto_(endLabel);
    }

    /**
     * @throws FlowNotStartedException if the flow operation has not been
     *                                 started yet.
     */
    protected void requireStarted() {
        if (!isStarted())
            throw new FlowNotStartedException();
    }

    /**
     * @throws FlowAlreadyStartedException if the flow operation has already
     *                                     been started.
     */
    protected void requireNotStarted() {
        if (isStarted())
            throw new FlowAlreadyStartedException();
    }

    /**
     * Requires the flow to be started, and its scope to be not closed.
     *
     * @throws FlowNotStartedException if the flow operation has not been
     *                                 started yet.
     * @throws ScopeAlreadyClosedException if this flow's scope is already
     *                                     closed.
     * @see #requireStarted()
     * @see ScopeManager.Scope#requireNotClosed()
     */
    protected void requireActive() {
        requireStarted();
        getScope().requireNotClosed();
    }


    /**
     * Closes this flow by binding its end label and closing its scope.
     *
     * @throws FlowNotStartedException if this flow has not been started yet.
     * @throws ScopeAlreadyClosedException if this flow's scope is already
     *                                     closed.
     */
    @Override
    public void close() {
        requireActive();

        getScope().getBuilder()
                .labelBinding(endLabel);

        getScope().close();
    }
}
