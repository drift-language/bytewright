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


/**
 * Thrown when a name that is a ready bound is attempted to be bind another
 * time.
 *
 * @author Jonathan (GitHub: belicfr)
 */
public class AlreadyBoundNameException extends BytewrightException {

    /**
     * Creates the exception for the already-bound {@code name}.
     *
     * @param name the name that is already bound.
     */
    public AlreadyBoundNameException(final String name) {
        super("Name '%s' is already bound in this scope.".formatted(name));
    }
}
