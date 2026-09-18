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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public class UnexpectedScopeException extends RuntimeException {

    @NonNull private final String reason;


    @Override
    public String getMessage() {
        return "Unexpected Scope: %s".formatted(reason);
    }
}
