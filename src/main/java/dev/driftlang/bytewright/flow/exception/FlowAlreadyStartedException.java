/*******************************************************************************
 * Drift Programming Language
 * ByteWright Library
 *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)
 *
 * This source code is licensed under the Apache 2.0 License.
 * See the LICENSE file in the root directory for details.
 ******************************************************************************/

package dev.driftlang.bytewright.flow.exception;

import dev.driftlang.bytewright.exception.BytewrightException;
import dev.driftlang.bytewright.flow.Flow;


/**
 * Thrown when an already started {@link Flow} has been tried to be started.
 * A flow can be started once.
 *
 * @author Jonathan (GitHub: belicfr)
 */
public class FlowAlreadyStartedException extends BytewrightException {

    /** Creates the exception for an already started flow. */
    public FlowAlreadyStartedException() {
        super("The flow operation is already started.");
    }
}
