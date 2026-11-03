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


/**
 * Thrown when an action needing a loop body is performed outside any loop body.
 *
 * @author Jonathan (GitHub: belicfr)
 */
public class NotInLoopBodyException extends BytewrightException {

    /**
     * Creates the exception for a loop-based action that is not in a loop body.
     */
    public NotInLoopBodyException() {
        super("This action can only be performed in a loop body.");
    }
}
