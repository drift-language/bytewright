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

import dev.driftlang.bytewright.scope.ScopeManager;
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
     */
    public void start() {
        getScope().requireNotClosed();

        super.start();

        getScope().getBuilder()
                .labelBinding(bodyLabel);
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
}
