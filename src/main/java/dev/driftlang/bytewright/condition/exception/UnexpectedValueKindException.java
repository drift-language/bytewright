/*******************************************************************************
 * Drift Programming Language
 * ByteWright Library
 *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)
 *
 * This source code is licensed under the Apache 2.0 License.
 * See the LICENSE file in the root directory for details.
 ******************************************************************************/

package dev.driftlang.bytewright.condition.exception;

import dev.driftlang.bytewright.exception.BytewrightException;
import java.lang.classfile.TypeKind;


public class UnexpectedValueKindException extends BytewrightException {

    public UnexpectedValueKindException(final TypeKind expected, final TypeKind actual) {
        super("Unexpected value kind: %s instead of %s."
                .formatted(actual.name(), expected.name()));
    }
}
