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
import lombok.Getter;
import lombok.NonNull;

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
    @NonNull protected final ScopeManager.Scope scope;

    @NonNull protected final Label endLabel;

    @Getter
    private boolean started = false;


    protected Flow(@NonNull final ScopeManager.Scope parentScope) {
        this.scope = parentScope.scope();
        this.endLabel = getScope().getBuilder().newLabel();
    }


    /**
     * Starts the flow operation. Can only be called once.
     */
    protected void start() {
        requireNotStarted();

        started = true;
    }


    @Override
    public void close() {
        requireStarted();
        getScope().requireNotClosed();

        getScope().getBuilder()
                .labelBinding(endLabel);

        getScope().close();
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
}
