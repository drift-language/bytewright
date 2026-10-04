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
import lombok.NonNull;

import java.lang.classfile.Label;


/**
 * Represents a control flow loop that contains some code to be repeated.
 * Being a minimal and shape-agnostic primitive, this class expects its usages
 * to be wrapped with appropriate actions, like a condition evaluation.
 *
 * @author Jonathan (GitHub: belicfr)
 */
public class Loop extends Flow {

    @NonNull private final Label bodyLabel;


    public Loop(@NonNull final ScopeManager.Scope parentScope) {
        super(parentScope);
        this.bodyLabel = getScope().getBuilder().newLabel();
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

        getScope().getBuilder()
                .labelBinding(bodyLabel);
    }

    /**
     * Continues this loop by generating a goto to its body label.
     *
     * @throws FlowNotStartedException if this flow has not been started yet.
     * @throws ScopeAlreadyClosedException if this flow's scope is already
     *                                     closed.
     */
    public void continue_() {
        requireStarted();
        getScope().requireNotClosed();

        getScope().getBuilder()
                .goto_(bodyLabel);
    }
}
