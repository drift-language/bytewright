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
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;


public class UnexpectedValueKindException extends BytewrightException {

    public UnexpectedValueKindException(final TypeKind expected, final TypeKind actual) {
        this(new TypeKind[]{ expected }, actual);
    }

    public UnexpectedValueKindException(final TypeKind[] supported, final TypeKind actual) {
        final String supportedTypes = stream(supported)
                .map(Enum::name)
                .collect(joining(" or "));

        super("Unexpected value kind: %s instead of %s."
                .formatted(actual.name(), supportedTypes));
    }
}
