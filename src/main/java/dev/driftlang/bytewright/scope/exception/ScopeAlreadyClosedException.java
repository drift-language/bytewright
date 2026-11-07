/*******************************************************************************
 * Drift Programming Language
 * Bytewright Library
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
 * Thrown when a {@link Scope} is used after it has been closed.
 * <p>
 * A closed scope has released its slots, so emitting through it would reference
 * slots that may already be reused.
 *
 * @author Jonathan (GitHub: belicfr)
 */
public class ScopeAlreadyClosedException extends BytewrightException {

    /**
     * Creates the exception for an already closed {@link Scope} tried to be
     * closed another time.
     */
    public ScopeAlreadyClosedException() {
        super("This scope is already closed and cannot be used anymore.");
    }
}
