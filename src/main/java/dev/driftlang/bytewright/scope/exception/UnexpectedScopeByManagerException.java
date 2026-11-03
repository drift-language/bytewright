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

import dev.driftlang.bytewright.scope.ScopeManager.Scope;

/**
 * Thrown when a {@link Scope} is used by another manager than its own. A scope
 * belongs to only one manager.
 *
 * @author Jonathan (GitHub: belicfr)
 */
public class UnexpectedScopeByManagerException extends UnexpectedScopeException {

    /**
     * Creates the exception for a scope used in a wrong manager.
     */
    public UnexpectedScopeByManagerException() {
        super("the provided scope does not belong to the same provided manager.");
    }
}
