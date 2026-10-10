/*******************************************************************************
 * Drift Programming Language
 * ByteWright Library
 *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)
 *
 * This source code is licensed under the Apache 2.0 License.
 * See the LICENSE file in the root directory for details.
 ******************************************************************************/

package dev.driftlang.bytewright.core;


import java.lang.classfile.CodeBuilder;


/**
 * Something that knows how to push itself onto the operand stack.
 * <p>
 * This is ByteWright's entry point and its inversion-of-control boundary:
 * the consuming language implements {@code Value} for its own expressions,
 * so ByteWright never sees that language's IR, only its {@link JvmType} and
 * the code that materializes it.
 * <p>
 * Implementations write directly into the caller's {@link CodeBuilder};
 * the raw builder is never hidden.
 *
 * @author Jonathan (GitHub: belicfr)
 */
public interface Value {

    /**
     * The type of value to be emitted.
     *
     * @return the {@link JvmType} associated with this value.
     */
    JvmType type();

    /**
     * Emits this value into the provided code builder.
     *
     * @param builder the {@link CodeBuilder} to push onto.
     */
    void emit(final CodeBuilder builder);
}
