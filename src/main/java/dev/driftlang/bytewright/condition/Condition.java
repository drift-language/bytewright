/*******************************************************************************
 * Drift Programming Language
 * Bytewright Library
 *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)
 *
 * This source code is licensed under the Apache 2.0 License.
 * See the LICENSE file in the root directory for details.
 ******************************************************************************/

package dev.driftlang.bytewright.condition;

import dev.driftlang.bytewright.core.Value;

import java.lang.classfile.CodeBuilder;
import java.lang.classfile.Label;


/**
 * A boolean condition emitted as a conditional jump, rather than as a value
 * pushed onto the operand stack. This interface can be used in a control flow
 * statement, a conditioned loop (e.g. {@code while}, {@code for}) or a boolean
 * value through {@link Value}.
 * <p>
 * Implementations must:
 * <ul>
 *     <li>
 *         leave the operand stack unchanged, on both paths;
 *     </li>
 *     <li>
 *         only jumps to the dedicated label, never bind it.
 *     </li>
 * </ul>
 *
 * @author Jonathan (GitHub: belicfr)
 */
@FunctionalInterface
public interface Condition {

    /**
     * Shortcut method of {@link #jump(CodeBuilder, Label, boolean)}. Jumps to
     * {@code target} if this condition is true and falls through otherwise.
     *
     * @param builder the instance of {@link CodeBuilder} to use to write
     *                the instructions.
     * @param target the label to jump to when the result is true.
     * @see #jump(CodeBuilder, Label, boolean)
     */
    default void jumpIfTrue(final CodeBuilder builder, final Label target) {
        jump(builder, target, true);
    }

    /**
     * Shortcut method of {@link #jump(CodeBuilder, Label, boolean)}. Jumps to
     * {@code target} if this condition is false, and falls through otherwise.
     *
     * @param builder the instance of {@link CodeBuilder} to use to write
     *                the instructions.
     * @param target the label to jump to when the result is false.
     * @see #jump(CodeBuilder, Label, boolean)
     */
    default void jumpIfFalse(final CodeBuilder builder, final Label target) {
        jump(builder, target, false);
    }

    /**
     * Emits to the provided {@link CodeBuilder} instance a jump instruction to
     * {@code target} label if the condition evaluation boolean result equals
     * the {@code when} parameter, and falls through to the next instruction
     * otherwise.
     *
     * @param builder the instance of {@link CodeBuilder} to use to write
     *                the instructions.
     * @param target the label to jump to when the result equals {@code when}.
     * @param when the result for which to jump.
     */
    void jump(final CodeBuilder builder, final Label target, final boolean when);
}