/*******************************************************************************
 * Drift Programming Language
 * ByteWright Library
 *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)
 *
 * This source code is licensed under the Apache 2.0 License.
 * See the LICENSE file in the root directory for details.
 ******************************************************************************/

package dev.driftlang.bytewright.condition;

import dev.driftlang.bytewright.condition.exception.UnexpectedValueKindException;
import dev.driftlang.bytewright.core.Value;
import lombok.NoArgsConstructor;

import java.lang.classfile.CodeBuilder;
import java.lang.classfile.Label;
import java.lang.classfile.Opcode;
import java.lang.classfile.TypeKind;

import static java.lang.classfile.Opcode.*;
import static java.util.Arrays.stream;
import static lombok.AccessLevel.PRIVATE;


/**
 * Factory methods that build and combine {@link Condition} instances, either
 * from other conditions or values.
 * <p>
 * The returned conditions only emit bytecode when their {@link
 * Condition#jump(CodeBuilder, Label, boolean) jump} method is called; building
 * them emits nothing.
 *
 * @author Jonathan (GitHub: belicfr)
 */
@NoArgsConstructor(access = PRIVATE)
public final class Conditions {

    /*
            FROM OTHER CONDITIONS
     */

    /**
     * Returns a new {@link Condition} that is true exactly when {@code
     * condition} is false.
     *
     * @param condition the condition object to negate.
     * @return the created condition object.
     */
    public static Condition not(final Condition condition) {
        return (builder, target, when) ->
                condition.jump(builder, target, !when);
    }

    /**
     * Returns a new {@link Condition} that is true if both {@code left} and
     * {@code right} are true. Short-circuit: {@code right} is only evaluated
     * if {@code left} is true.
     *
     * @param left  the condition evaluated first.
     * @param right the condition evaluated only if {@code left} is true.
     * @return the created condition object that is the conjunction of {@code
     * left} and {@code right}.
     */
    public static Condition and(final Condition left, final Condition right) {
        return (builder, target, when) -> {
            if (!when) {
                left.jumpIfFalse(builder, target);
                right.jumpIfFalse(builder, target);

                return;
            }

            Label skipLabel = builder.newLabel();

            left.jumpIfFalse(builder, skipLabel);
            // NOTE: left == false -> the whole expression is false -> skip the
            //  right's evaluation.

            right.jumpIfTrue(builder, target);
            // NOTE: if this line is reached, left is true.

            builder.labelBinding(skipLabel);
        };
    }

    /**
     * Returns a new {@link Condition} that is true if {@code left} or {@code
     * right} or both are true.
     *
     * @param left  the condition evaluated first.
     * @param right the condition evaluated only if {@code left} is false.
     * @return the created condition object that is the disjunction of {@code
     * left} and {@code right}.
     */
    public static Condition or(final Condition left, final Condition right) {
        return (builder, target, when) -> {
            if (!when) {
                and(not(left), not(right))
                        .jumpIfTrue(builder, target);
                // NOTE: De Morgan's law:
                //  ! ( left || right )  ==  ( ! left && ! right )

                return;
            }

            left.jumpIfTrue(builder, target);
            // NOTE: left == true -> the whole expression is true -> skip the
            //  right's evaluation by jumping straight to target.

            right.jumpIfTrue(builder, target);
            // NOTE: if this line is reached, left is false.
        };
    }


    /*
            FROM VALUES
     */

    /**
     * Returns a new {@link Condition} that is true if {@code value} is non-zero
     * (true). Since the JVM represents booleans as integers, {@code value}
     * must be of an int-category type.
     *
     * @param value the value to evaluate.
     * @return the created condition object.
     * @see Opcode#IFEQ
     * @see Opcode#IFNE
     */
    public static Condition isTrue(final Value value) {
        requireBooleanable(value);

        return (builder, target, when) -> {
            value.emit(builder);

            builder.branch(when ? IFNE : IFEQ, target);
        };
    }

    /**
     * Returns a new {@link Condition} that is true if {@code value} is null.
     * According to the used {@code IFNULL / IFNONNULL} opcodes, {@code value}
     * must be of a reference type (object or array).
     *
     * @param value the value to evaluate.
     * @return the created condition object.
     * @see Opcode#IFNULL
     * @see Opcode#IFNONNULL
     */
    public static Condition isNull(final Value value) {
        requireReference(value);
        // NOTE: an array is considered as a reference.

        return (builder, target, when) -> {
            value.emit(builder);

            builder.branch(when ? IFNULL : IFNONNULL, target);
        };
    }


    /**
     * Requires the given {@code value} to be of a boolean-compatible type and
     * throws otherwise.
     *
     * @param value the value to check.
     * @throws UnexpectedValueKindException if the value is not an integer.
     */
    private static void requireBooleanable(final Value value) {
        final TypeKind[] booleanableTypes = new TypeKind[]{
                TypeKind.BOOLEAN,
                TypeKind.BYTE,
                TypeKind.SHORT,
                TypeKind.CHAR,
                TypeKind.INT
        };
        final TypeKind actualKind = value.type().kind();

        requireValueKind(booleanableTypes, actualKind);
    }

    /**
     * Requires the given {@code value} to be a {@link TypeKind#REFERENCE
     * reference} and throws otherwise.
     * <p>
     * <strong>IMPORTANT:</strong> JVM considers an array as a reference.
     *
     * @param value the value to check.
     * @throws UnexpectedValueKindException if the value is not a reference or
     *                                      an array.
     */
    private static void requireReference(final Value value) {
        final TypeKind integerKind = TypeKind.REFERENCE;
        final TypeKind actualKind = value.type().kind();

        requireValueKind(integerKind, actualKind);
    }

    /**
     * Requires the given {@code actual} {@link TypeKind} to be included into
     * the {@code supported} array and throws otherwise.
     *
     * @param supported the supported kinds array where the actual value's type
     *                  must be included.
     * @param actual the actual kind that must be included in the supported
     *               types array.
     * @throws UnexpectedValueKindException if the constraint is not respected.
     */
    private static void requireValueKind(final TypeKind[] supported, final TypeKind actual) {
        boolean isActualTypeSupported = stream(supported)
                .anyMatch(s -> s == actual);

        if (!isActualTypeSupported)
            throw new UnexpectedValueKindException(supported, actual);
    }

    /**
     * Requires the given {@code actual} {@link TypeKind} to equals the
     * {@code required} one and throws otherwise.
     *
     * @param required the required kind that the actual must equal.
     * @param actual the actual kind that must equal the required one.
     * @throws UnexpectedValueKindException if the constraint is not respected.
     */
    private static void requireValueKind(final TypeKind required, final TypeKind actual) {
        if (actual != required)
            throw new UnexpectedValueKindException(required, actual);
    }
}