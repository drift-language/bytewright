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
public class AlreadyBoundNameException extends RuntimeException {

    @NonNull private final String name;


    @Override
    public String getMessage() {
        return "Name '%s' is already bound in this scope.".formatted(name);
    }
}
