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

import lombok.NoArgsConstructor;

import java.lang.classfile.CodeBuilder;
import java.lang.classfile.Label;

import static lombok.AccessLevel.PRIVATE;


/**
 * Factory methods that build and combine {@link Condition} instances, either
 * from other conditions or, later, from values.
 * <p>
 * The returned conditions only emit bytecode when their {@link
 * Condition#jump(CodeBuilder, Label, boolean) jump} method is called; building
 * them emits nothing.
 *
 * @author Jonathan (GitHub: belicfr)
 */
@NoArgsConstructor(access = PRIVATE)
public final class Conditions {

    /**
     * Returns a new {@link Condition} that is true exactly when {@code
     * condition} is false.
     *
     * @param condition the condition object to negate.
     * @return the created condition object.
     */
    public static Condition not(final Condition condition) {
        return (builder, target, when) -> {
            condition.jump(builder, target, !when);
        };
    }

     /**
      * Returns a new {@link Condition} that is true if both {@code left} and
      * {@code right} are true. Short-circuit: {@code right} is only evaluated
      * if {@code left} is true.
      *
      * @param left the condition evaluated first.
      * @param right the condition evaluated only if {@code left} is true.
      * @return the created condition object that is the conjunction of {@code
      *         left} and {@code right}.
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
     * @param left the condition evaluated first.
     * @param right the condition evaluated only if {@code left} is false.
     * @return the created condition object that is the disjunction of {@code
     *         left} and {@code right}.
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
            //  right's evaluation.

            right.jumpIfTrue(builder, target);
            // NOTE: if this line is reached, left is true.
        };
    }
}
