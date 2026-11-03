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
 * Thrown when a value needing a switch case does not have its own.
 *
 * @author Jonathan (GitHub: belicfr)
 */
public class UnregisteredSwitchCaseValueException extends BytewrightException {

    /**
     * @param value the value that does not have a switch case.
     */
    public UnregisteredSwitchCaseValueException(final int value) {
        super("No case is registered for value %d.".formatted(value));
    }
}
