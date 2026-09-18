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

import dev.driftlang.bytewright.flow.exception.LoopAlreadyStartedException;
import dev.driftlang.bytewright.flow.exception.LoopNotStartedException;
import dev.driftlang.bytewright.scope.ScopeManager;
import lombok.Getter;
import lombok.NonNull;

import java.lang.classfile.Label;


/**
 * Represents a control flow loop that contains some code to be repeated.
 * Being a minimal and shape-agnostic primitive, this class expects its usages
 * to be wrapped with appropriate actions, like a condition evaluation.
 *
 * @author Jonathan (GitHub: belicfr)
 */
public class Loop implements AutoCloseable {

    @Getter
    @NonNull private final ScopeManager.Scope scope;

    @NonNull private final Label bodyLabel;
    @NonNull private final Label endLabel;

    @Getter
    private boolean started = false;


    public Loop(@NonNull final ScopeManager.Scope parentScope) {
        this.scope = parentScope.scope();
        this.bodyLabel = getScope().getBuilder().newLabel();
        this.endLabel = getScope().getBuilder().newLabel();
    }


    /**
     * Starts this loop by binding its body label and marking it as started.
     */
    public void start() {
        getScope().requireNotClosed();
        requireNotStarted();

        getScope().getBuilder()
                .labelBinding(bodyLabel);

        started = true;
    }

    /**
     * Continues this loop by generating a goto to its body label.
     */
    public void continue_() {
        getScope().requireNotClosed();
        requireStarted();

        getScope().getBuilder()
                .goto_(bodyLabel);
    }

    /**
     * Breaks this loop by generating a goto to its end label.
     */
    public void break_() {
        getScope().requireNotClosed();
        requireStarted();

        getScope().getBuilder()
                .goto_(endLabel);
    }


    @Override
    public void close() {
        getScope().requireNotClosed();

        getScope().getBuilder()
                .labelBinding(endLabel);

        getScope().close();
    }


    /**
     * @throws LoopAlreadyStartedException if the loop has already been started.
     */
    private void requireNotStarted() {
        if (isStarted())
            throw new LoopAlreadyStartedException();
    }

    /**
     * @throws LoopNotStartedException if the loop has not been started yet.
     */
    private void requireStarted() {
        if (!isStarted())
            throw new LoopNotStartedException();
    }
}
