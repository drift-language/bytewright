/*******************************************************************************
 * Drift Programming Language
 * Bytewright Library
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
public class UnregisteredSwitchCaseByValueException extends BytewrightException {

    /**
     * Creates the exception for an unregistered switch case with a given value.
     *
     * @param value the value that does not have a switch case.
     */
    public UnregisteredSwitchCaseByValueException(final int value) {
        super("No case is registered for value %d.".formatted(value));
    }
}
