/*******************************************************************************
 * Drift Programming Language
 * ByteWright Library
 *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)
 *
 * This source code is licensed under the Apache 2.0 License.
 * See the LICENSE file in the root directory for details.
 ******************************************************************************/

package dev.driftlang.bytewright.exception;


/**
 * Base class of every exception thrown by Bytewright.
 * <p>
 * All Bytewright exceptions signal a misuse of the API at emission time, never
 * a recoverable condition. Hence, they are unchecked.
 *
 * @author Jonathan (GitHub: belicfr)
 */
public abstract class BytewrightException extends RuntimeException {

    /**
     * Creates the Bytewright exception with the provided {@code message}.
     *
     * @param message the exception's message.
     */
    public BytewrightException(final String message) {
        super(message);
    }
}
