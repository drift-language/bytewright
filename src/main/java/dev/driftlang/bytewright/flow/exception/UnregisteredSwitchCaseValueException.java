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

import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public class UnregisteredSwitchCaseValueException extends RuntimeException {

    private final int value;


    @Override
    public String getMessage() {
        return "No case is registered for value %d.".formatted(value);
    }
}
