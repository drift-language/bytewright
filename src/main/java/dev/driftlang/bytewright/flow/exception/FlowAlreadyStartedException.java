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

public class FlowAlreadyStartedException extends RuntimeException {

    @Override
    public String getMessage() {
        return "The flow operation is already started.";
    }
}
