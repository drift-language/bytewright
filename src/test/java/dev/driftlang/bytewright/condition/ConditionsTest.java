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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.classfile.ClassFile;
import java.lang.classfile.Label;
import java.lang.classfile.Opcode;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;

import static dev.driftlang.bytewright.condition.Conditions.and;
import static dev.driftlang.bytewright.condition.Conditions.not;
import static dev.driftlang.bytewright.condition.Conditions.or;
import static java.lang.constant.ConstantDescs.CD_boolean;
import static java.lang.constant.ConstantDescs.CD_Object;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * The operators are checked by running them: each test generates a real
 * {@code static boolean test(boolean a, boolean b)} method, loads it as a
 * hidden class and calls it. The generated method returns {@code true} if
 * the condition jumped to its target, and {@code false} if it fell through.
 */
class ConditionsTest {

    private static final ClassDesc HOST = ClassDesc.of("dev.driftlang.bytewright.condition.GeneratedConditionHost");

    private static final MethodType TEST_TYPE = MethodType.methodType(boolean.class, boolean.class, boolean.class);

    /** Leaf condition reading the first {@code boolean} parameter. */
    private static final Condition A = parameter(0);

    /** Leaf condition reading the second {@code boolean} parameter. */
    private static final Condition B = parameter(1);

    /**
     * Leaf condition whose evaluation throws an {@link ArithmeticException}
     * at runtime (integer division by zero). Used to prove that an operand
     * is never evaluated.
     */
    private static final Condition EXPLODING = (builder, target, when) -> {
        builder.iconst_1()
                .iconst_0()
                .idiv()
                .branch(when ? Opcode.IFNE : Opcode.IFEQ, target);
    };


    @Nested
    class ParameterLeaf {

        @ParameterizedTest(name = "a={0}, when={1}")
        @CsvSource({
                "false, false",
                "false, true",
                "true,  false",
                "true,  true",
        })
        void jumpsWhenResultEqualsWhen(final boolean a, final boolean when) throws Throwable {
            // GIVEN the test leaf condition on parameter a
            // WHEN it is emitted and run
            boolean jumped = jumps(A, when, a, false);

            // THEN it jumps exactly when a equals when (sanity check of the harness)
            assertThat(jumped).isEqualTo(a == when);
        }
    }


    @Nested
    class Not {

        @ParameterizedTest(name = "a={0}, when={1}")
        @CsvSource({
                "false, false",
                "false, true",
                "true,  false",
                "true,  true",
        })
        void jumpsWhenNegatedResultEqualsWhen(final boolean a, final boolean when) throws Throwable {
            // GIVEN
            Condition condition = not(A);

            // WHEN
            boolean jumped = jumps(condition, when, a, false);

            // THEN
            assertThat(jumped).isEqualTo(!a == when);
        }

        @ParameterizedTest(name = "a={0}, when={1}")
        @CsvSource({
                "false, false",
                "false, true",
                "true,  false",
                "true,  true",
        })
        void appliedTwice_isIdentity(final boolean a, final boolean when) throws Throwable {
            // GIVEN
            Condition condition = not(not(A));

            // WHEN
            boolean jumped = jumps(condition, when, a, false);

            // THEN
            assertThat(jumped).isEqualTo(a == when);
        }
    }


    @Nested
    class And {

        @ParameterizedTest(name = "a={0}, b={1}, when={2}")
        @CsvSource({
                "false, false, false",
                "false, true,  false",
                "true,  false, false",
                "true,  true,  false",
                "false, false, true",
                "false, true,  true",
                "true,  false, true",
                "true,  true,  true",
        })
        void jumpsWhenConjunctionEqualsWhen(final boolean a, final boolean b, final boolean when) throws Throwable {
            // GIVEN
            Condition condition = and(A, B);

            // WHEN
            boolean jumped = jumps(condition, when, a, b);

            // THEN
            assertThat(jumped).isEqualTo((a && b) == when);
        }

        @ParameterizedTest(name = "when={0}")
        @CsvSource({"false", "true"})
        void withFalseLeft_doesNotEvaluateRight(final boolean when) throws Throwable {
            // GIVEN a false left operand and a right operand that throws if run
            Condition condition = and(A, EXPLODING);

            // WHEN
            boolean jumped = jumps(condition, when, false, false);

            // THEN right was skipped, and the result is false
            assertThat(jumped).isEqualTo(!when);
        }

        @ParameterizedTest(name = "when={0}")
        @CsvSource({"false", "true"})
        void withTrueLeft_evaluatesRight(final boolean when) {
            // GIVEN a true left operand and a right operand that throws if run
            // WHEN / THEN right is evaluated (control for the short-circuit test)
            assertThatThrownBy(() -> jumps(and(A, EXPLODING), when, true, false))
                    .isInstanceOf(ArithmeticException.class);
        }
    }


    @Nested
    class Or {

        @ParameterizedTest(name = "a={0}, b={1}, when={2}")
        @CsvSource({
                "false, false, false",
                "false, true,  false",
                "true,  false, false",
                "true,  true,  false",
                "false, false, true",
                "false, true,  true",
                "true,  false, true",
                "true,  true,  true",
        })
        void jumpsWhenDisjunctionEqualsWhen(final boolean a, final boolean b, final boolean when) throws Throwable {
            // GIVEN
            Condition condition = or(A, B);

            // WHEN
            boolean jumped = jumps(condition, when, a, b);

            // THEN
            assertThat(jumped).isEqualTo((a || b) == when);
        }

        @ParameterizedTest(name = "when={0}")
        @CsvSource({"false", "true"})
        void withTrueLeft_doesNotEvaluateRight(final boolean when) throws Throwable {
            // GIVEN a true left operand and a right operand that throws if run
            Condition condition = or(A, EXPLODING);

            // WHEN
            boolean jumped = jumps(condition, when, true, false);

            // THEN right was skipped, and the result is true
            assertThat(jumped).isEqualTo(when);
        }

        @ParameterizedTest(name = "when={0}")
        @CsvSource({"false", "true"})
        void withFalseLeft_evaluatesRight(final boolean when) {
            // GIVEN a false left operand and a right operand that throws if run
            // WHEN / THEN right is evaluated (control for the short-circuit test)
            assertThatThrownBy(() -> jumps(or(A, EXPLODING), when, false, false))
                    .isInstanceOf(ArithmeticException.class);
        }
    }


    @Nested
    class IsTrue {
    }


    @Nested
    class IsNull {
    }


    @Nested
    class Composition {

        @ParameterizedTest(name = "a={0}, b={1}, when={2}")
        @CsvSource({
                "false, false, false",
                "false, true,  false",
                "true,  false, false",
                "true,  true,  false",
                "false, false, true",
                "false, true,  true",
                "true,  false, true",
                "true,  true,  true",
        })
        void xorExpression_jumpsWhenResultEqualsWhen(final boolean a, final boolean b, final boolean when) throws Throwable {
            // GIVEN (a || b) && !(a && b), i.e. a XOR b
            Condition condition = and(or(A, B), not(and(A, B)));

            // WHEN
            boolean jumped = jumps(condition, when, a, b);

            // THEN
            assertThat(jumped).isEqualTo((a ^ b) == when);
        }

        @ParameterizedTest(name = "a={0}, b={1}")
        @CsvSource({
                "false, false",
                "false, true",
                "true,  false",
                "true,  true",
        })
        void sameConditionEmittedTwice_usesFreshLabels(final boolean a, final boolean b) throws Throwable {
            // GIVEN one and(...) instance, emitted twice in the same method:
            // jumpIfTrue, then jumpIfFalse on its fall-through path. Each
            // emission needs its own skip label, or the second binding fails.
            Condition condition = and(A, B);

            byte[] bytes = ClassFile.of().build(HOST, classBuilder -> classBuilder
                    .withSuperclass(CD_Object)
                    .withMethodBody(
                            "test",
                            MethodTypeDesc.of(CD_boolean, CD_boolean, CD_boolean),
                            ClassFile.ACC_PUBLIC | ClassFile.ACC_STATIC,
                            codeBuilder -> {
                                Label isTrue = codeBuilder.newLabel();
                                Label isFalse = codeBuilder.newLabel();

                                condition.jumpIfTrue(codeBuilder, isTrue);
                                condition.jumpIfFalse(codeBuilder, isFalse);

                                codeBuilder.iconst_0()   // unreachable: one of the two jumps is always taken
                                        .ireturn()
                                        .labelBinding(isTrue)
                                        .iconst_1()
                                        .ireturn()
                                        .labelBinding(isFalse)
                                        .iconst_0()
                                        .ireturn();
                            }));

            MethodHandles.Lookup lookup = MethodHandles.lookup().defineHiddenClass(bytes, true);
            MethodHandle test = lookup.findStatic(lookup.lookupClass(), "test", TEST_TYPE);

            // WHEN
            boolean result = (boolean) test.invokeExact(a, b);

            // THEN
            assertThat(result).isEqualTo(a && b);
        }

        @Test
        void building_emitsNothing() {
            // GIVEN leaves that record every time they are asked to emit
            List<String> emitted = new ArrayList<>();
            Condition left = (builder, target, when) -> emitted.add("left");
            Condition right = (builder, target, when) -> emitted.add("right");

            // WHEN building a nested condition without calling jump
            Condition condition = or(and(left, not(right)), not(left));

            // THEN nothing has been emitted yet
            assertSoftly(softly -> {
                softly.assertThat(condition).isNotNull();
                softly.assertThat(emitted).isEmpty();
            });
        }
    }


    /**
     * Returns a leaf condition testing the {@code boolean} parameter at
     * {@code slot}.
     */
    private static Condition parameter(final int slot) {
        return (builder, target, when) -> {
            builder.iload(slot)
                    .branch(when ? Opcode.IFNE : Opcode.IFEQ, target);
        };
    }

    /**
     * Generates and loads a {@code test(a, b)} method that emits
     * {@code condition} with the given {@code when}, and returns whether
     * the jump was taken.
     */
    private static MethodHandle compile(final Condition condition, final boolean when) throws Exception {
        byte[] bytes = ClassFile.of().build(HOST, classBuilder -> classBuilder
                .withSuperclass(CD_Object)
                .withMethodBody(
                        "test",
                        MethodTypeDesc.of(CD_boolean, CD_boolean, CD_boolean),
                        ClassFile.ACC_PUBLIC | ClassFile.ACC_STATIC,
                        codeBuilder -> {
                            Label jumped = codeBuilder.newLabel();

                            condition.jump(codeBuilder, jumped, when);

                            codeBuilder.iconst_0()
                                    .ireturn()
                                    .labelBinding(jumped)
                                    .iconst_1()
                                    .ireturn();
                        }));

        MethodHandles.Lookup lookup = MethodHandles.lookup().defineHiddenClass(bytes, true);

        return lookup.findStatic(lookup.lookupClass(), "test", TEST_TYPE);
    }

    /**
     * Runs {@code condition} emitted with {@code when}, on inputs {@code a}
     * and {@code b}.
     */
    private static boolean jumps(final Condition condition, final boolean when,
                                 final boolean a, final boolean b) throws Throwable {
        return (boolean) compile(condition, when).invokeExact(a, b);
    }
}
