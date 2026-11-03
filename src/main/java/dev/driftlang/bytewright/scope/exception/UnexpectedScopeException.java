/*******************************************************************************
 * Drift Programming Language
 * ByteWright Library
 *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)
 *
 * This source code is licensed under the Apache 2.0 License.
 * See the LICENSE file in the root directory for details.
 ******************************************************************************/

package dev.driftlang.bytewright.scope.exception;

import dev.driftlang.bytewright.exception.BytewrightException;
import dev.driftlang.bytewright.scope.ScopeManager.Scope;


/**
 * Thrown when a {@link Scope} is unexpected in the current context.
 *
 * @author Jonathan (GitHub: belicfr)
 */
public class UnexpectedScopeException extends BytewrightException {

    /**
     * Creates the exception for an unexpected scope.
     *
     * @param reason more details about the unexpected situation.
     */
    public UnexpectedScopeException(String reason) {
        super("Unexpected Scope: %s".formatted(reason));
    }
}
