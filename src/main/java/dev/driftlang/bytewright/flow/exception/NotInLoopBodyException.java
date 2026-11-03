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


public class NotInLoopBodyException extends BytewrightException {

    public NotInLoopBodyException() {
        super("This action can only be performed in a loop body.");
    }
}
