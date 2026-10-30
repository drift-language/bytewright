/*******************************************************************************
 * Drift Programming Language
 * ByteWright Library
 *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)
 *
 * This source code is licensed under the Apache 2.0 License.
 * See the LICENSE file in the root directory for details.
 ******************************************************************************/

package dev.driftlang.bytewright.flow;

import dev.driftlang.bytewright.flow.exception.FlowAlreadyStartedException;
import dev.driftlang.bytewright.flow.exception.FlowNotStartedException;
import dev.driftlang.bytewright.flow.exception.NotInLoopBodyException;
import dev.driftlang.bytewright.scope.ScopeManager;
import dev.driftlang.bytewright.scope.exception.ScopeAlreadyClosedException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.classfile.ClassFile;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.Label;
import java.lang.classfile.MethodModel;
import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.BranchInstruction;
import java.lang.classfile.instruction.LabelTarget;
import java.lang.classfile.instruction.NopInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static java.lang.constant.ConstantDescs.CD_void;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * {@link Loop} can only be exercised against a real {@link CodeBuilder}
 * (labels and gotos are stateful and tied to an actual method body being
 * built), so every test here drives it through a throwaway method generated
 * with {@link ClassFile}.
 */
class LoopTest {

    private static final ClassDesc HOST = ClassDesc.of("dev.driftlang.bytewright.flow.GeneratedTestHost");


    /**
     * Builds a throwaway static {@code test()} method whose body is produced
     * by {@code body}, then returns the resulting method model for inspection.
     */
    private static MethodModel buildMethod(final Consumer<CodeBuilder> body) {
        byte[] bytes = ClassFile.of().build(HOST, classBuilder -> classBuilder.withMethodBody(
                "test",
                MethodTypeDesc.of(CD_void),
                ClassFile.ACC_STATIC,
                codeBuilder -> {
                    body.accept(codeBuilder);
                    codeBuilder.return_();
                }));

        return ClassFile.of()
                .parse(bytes)
                .methods()
                .getFirst();
    }

    /**
     * Returns the code elements (instructions and label targets) of
     * {@code method}, in emission order.
     */
    private static List<CodeElement> elementsOf(final MethodModel method) {
        return method.code()
                .orElseThrow()
                .elementList();
    }

    /**
     * Maps each label bound in {@code elements} to the index of its
     * {@link LabelTarget}.
     */
    private static Map<Label, Integer> labelPositions(final List<CodeElement> elements) {
        Map<Label, Integer> positions = new HashMap<>();

        for (int i = 0; i < elements.size(); i++)
            if (elements.get(i) instanceof LabelTarget target)
                positions.put(target.label(), i);

        return positions;
    }

    /**
     * Returns the indices of the {@code goto} instructions in
     * {@code elements}.
     */
    private static List<Integer> gotoPositions(final List<CodeElement> elements) {
        return IntStream.range(0, elements.size())
                .filter(i -> elements.get(i) instanceof BranchInstruction branch
                        && branch.opcode() == Opcode.GOTO)
                .boxed()
                .toList();
    }


    @Nested
    class Constructor {

        @Test
        void createsChildScopeOfParent() {
            // GIVEN
            final ScopeManager.Scope[] parent = new ScopeManager.Scope[1];
            final Loop[] loop = new Loop[1];

            // WHEN
            buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);
                parent[0] = manager.scope();
                loop[0] = new Loop(parent[0]);
            });

            // THEN
            assertSoftly(softly -> {
                softly.assertThat(loop[0].getScope().hasParent()).isTrue();
                softly.assertThat(loop[0].getScope().getParent()).isSameAs(parent[0]);
            });
        }
    }


    @Nested
    class IsStarted {

        @Test
        void beforeStart_isFalse() {
            // GIVEN / WHEN
            final Loop[] loop = new Loop[1];

            buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);
                loop[0] = new Loop(manager.scope());
            });

            // THEN
            assertThat(loop[0].isStarted()).isFalse();
        }
    }


    @Nested
    class Start {

        @Test
        void marksAsStarted() {
            // GIVEN
            final Loop[] loop = new Loop[1];

            // WHEN
            buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);
                loop[0] = new Loop(manager.scope());
                loop[0].start();
            });

            // THEN
            assertThat(loop[0].isStarted()).isTrue();
        }

        @Test
        void calledTwice_throwsFlowAlreadyStartedException() {
            // GIVEN a loop that has already been started
            // WHEN starting it a second time
            // THEN a FlowAlreadyStartedException is thrown
            assertThatThrownBy(() -> buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);
                Loop loop = new Loop(manager.scope());
                loop.start();

                loop.start();
            })).isInstanceOf(FlowAlreadyStartedException.class);
        }

        @Test
        void afterClose_throwsScopeAlreadyClosedException() {
            // GIVEN a loop that has already been closed
            // WHEN starting it
            // THEN a ScopeAlreadyClosedException is thrown
            assertThatThrownBy(() -> buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);
                Loop loop = new Loop(manager.scope());
                loop.start();
                loop.close();

                loop.start();
            })).isInstanceOf(ScopeAlreadyClosedException.class);
        }
    }


    @Nested
    class Continue {

        @Test
        void beforeStart_throwsFlowNotStartedException() {
            // GIVEN a loop that has not been started
            // WHEN continuing it
            // THEN a FlowNotStartedException is thrown
            assertThatThrownBy(() -> buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);
                Loop loop = new Loop(manager.scope());

                loop.continue_();
            })).isInstanceOf(FlowNotStartedException.class);
        }

        @Test
        void afterStart_doesNotThrow() {
            // GIVEN a started loop
            // WHEN continuing it, then completing the loop's lifecycle so the
            // referenced continue label actually gets bound
            // THEN no exception is thrown
            assertThatCode(() -> buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);
                try (Loop loop = new Loop(manager.scope())) {
                    loop.start();
                    loop.continue_();
                }
            })).doesNotThrowAnyException();
        }

        @Test
        void afterContinueEntry_throwsNotInLoopBodyException() {
            // GIVEN a started loop whose continue point has been entered
            // WHEN continuing it from within its step
            // THEN a NotInLoopBodyException is thrown
            assertThatThrownBy(() -> buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);
                Loop loop = new Loop(manager.scope());
                loop.start();
                loop.continueEntry();

                loop.continue_();
            })).isInstanceOf(NotInLoopBodyException.class);
        }

        @Test
        void afterClose_throwsScopeAlreadyClosedException() {
            // GIVEN a started loop that has already been closed
            // WHEN continuing it
            // THEN a ScopeAlreadyClosedException is thrown (checked before the
            // started state)
            assertThatThrownBy(() -> buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);
                Loop loop = new Loop(manager.scope());
                loop.start();
                loop.close();

                loop.continue_();
            })).isInstanceOf(ScopeAlreadyClosedException.class);
        }

        @Test
        void inSteppedLoop_jumpsToStep() {
            // GIVEN / WHEN a loop whose body continues, and whose step is marked
            // by a single nop
            MethodModel method = buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);

                try (Loop loop = new Loop(manager.scope())) {
                    loop.start();
                    loop.continue_();

                    loop.continueEntry();
                    builder.nop();
                }
            });

            // THEN the first goto (from continue_) jumps forwards, to the step
            List<CodeElement> elements = elementsOf(method);
            Map<Label, Integer> labels = labelPositions(elements);

            int continueGoto = gotoPositions(elements).getFirst();
            Label target = ((BranchInstruction) elements.get(continueGoto)).target();
            int targetIndex = Objects.requireNonNull(labels.get(target));

            assertSoftly(softly -> {
                softly.assertThat(targetIndex).isGreaterThan(continueGoto);
                softly.assertThat(elements.get(targetIndex + 1))
                        .isInstanceOf(NopInstruction.class);
            });
        }
    }


    @Nested
    class ContinueEntry {

        @Test
        void beforeStart_throwsFlowNotStartedException() {
            // GIVEN a loop that has not been started
            // WHEN entering its continue point
            // THEN a FlowNotStartedException is thrown
            assertThatThrownBy(() -> buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);
                Loop loop = new Loop(manager.scope());

                loop.continueEntry();
            })).isInstanceOf(FlowNotStartedException.class);
        }

        @Test
        void afterClose_throwsScopeAlreadyClosedException() {
            // GIVEN a started loop that has already been closed
            // WHEN entering its continue point
            // THEN a ScopeAlreadyClosedException is thrown
            assertThatThrownBy(() -> buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);
                Loop loop = new Loop(manager.scope());
                loop.start();
                loop.close();

                loop.continueEntry();
            })).isInstanceOf(ScopeAlreadyClosedException.class);
        }

        @Test
        void calledTwice_throwsIllegalArgumentException() {
            // GIVEN a started loop whose continue point has been entered
            // WHEN entering it a second time
            // THEN the classfile API rejects the second label binding eagerly
            assertThatThrownBy(() -> buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);
                Loop loop = new Loop(manager.scope());
                loop.start();
                loop.continueEntry();

                loop.continueEntry();
            })).isInstanceOf(IllegalArgumentException.class);
        }
    }


    @Nested
    class Break {

        @Test
        void beforeStart_throwsFlowNotStartedException() {
            // GIVEN a loop that has not been started
            // WHEN breaking it
            // THEN a FlowNotStartedException is thrown
            assertThatThrownBy(() -> buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);
                Loop loop = new Loop(manager.scope());

                loop.break_();
            })).isInstanceOf(FlowNotStartedException.class);
        }

        @Test
        void afterStart_doesNotThrow() {
            // GIVEN a started loop
            // WHEN breaking it, then completing the loop's lifecycle so the
            // referenced end label actually gets bound
            // THEN no exception is thrown
            assertThatCode(() -> buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);
                try (Loop loop = new Loop(manager.scope())) {
                    loop.start();
                    loop.break_();
                }
            })).doesNotThrowAnyException();
        }

        @Test
        void afterClose_throwsScopeAlreadyClosedException() {
            // GIVEN a started loop that has already been closed
            // WHEN breaking it
            // THEN a ScopeAlreadyClosedException is thrown (checked before the
            // started state)
            assertThatThrownBy(() -> buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);
                Loop loop = new Loop(manager.scope());
                loop.start();
                loop.close();

                loop.break_();
            })).isInstanceOf(ScopeAlreadyClosedException.class);
        }
    }


    @Nested
    class Close {

        @Test
        void withoutStart_throwsFlowNotStartedException() {
            // GIVEN a loop that has never been started
            // WHEN closing it
            // THEN a FlowNotStartedException is thrown
            assertThatThrownBy(() -> buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);
                Loop loop = new Loop(manager.scope());

                loop.close();
            })).isInstanceOf(FlowNotStartedException.class);
        }

        @Test
        void calledTwice_throwsScopeAlreadyClosedException() {
            // GIVEN a loop that has already been closed
            // WHEN closing it a second time
            // THEN a ScopeAlreadyClosedException is thrown
            assertThatThrownBy(() -> buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);
                Loop loop = new Loop(manager.scope());
                loop.start();
                loop.close();

                loop.close();
            })).isInstanceOf(ScopeAlreadyClosedException.class);
        }

        @Test
        void closesUnderlyingScope() {
            // GIVEN
            final Loop[] loop = new Loop[1];

            // WHEN
            buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);
                loop[0] = new Loop(manager.scope());
                loop[0].start();

                loop[0].close();
            });

            // THEN
            assertThat(loop[0].getScope().isClosed()).isTrue();
        }

        @Test
        void withoutContinueEntry_bindsContinueLabel() {
            // GIVEN a while-like loop that continues but never enters its
            // continue point
            // WHEN closing it
            // THEN the continue label is bound by close() and the method builds
            assertThatCode(() -> buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);

                try (Loop loop = new Loop(manager.scope())) {
                    loop.start();
                    loop.continue_();
                }
            })).doesNotThrowAnyException();
        }

        @Test
        void emitsBackEdgeToLoopHead() {
            // GIVEN / WHEN
            MethodModel method = buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);

                try (Loop loop = new Loop(manager.scope())) {
                    loop.start();
                    builder.nop();
                }
            });

            // THEN exactly one goto is emitted, and it jumps backwards
            List<CodeElement> elements = elementsOf(method);
            Map<Label, Integer> labels = labelPositions(elements);
            List<Integer> gotos = gotoPositions(elements);

            assertThat(gotos).hasSize(1);

            int gotoIndex = gotos.getFirst();
            Label target = ((BranchInstruction) elements.get(gotoIndex)).target();

            assertThat(labels.get(target)).isLessThan(gotoIndex);
        }
    }


    @Nested
    class FullLifecycle {

        @Test
        void startContinueBreakClose_doesNotThrow() {
            // GIVEN / WHEN / THEN a loop taken through its whole lifecycle builds
            // valid bytecode without throwing
            assertThatCode(() -> buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);

                try (Loop loop = new Loop(manager.scope())) {
                    loop.start();
                    loop.continue_();
                    loop.break_();
                }
            })).doesNotThrowAnyException();
        }

        @Test
        void steppedLoop_doesNotThrow() {
            // GIVEN / WHEN / THEN a stepped loop (body, continue point, step)
            // taken through its whole lifecycle builds without throwing
            assertThatCode(() -> buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);

                try (Loop loop = new Loop(manager.scope())) {
                    loop.start();
                    loop.continue_();
                    loop.break_();

                    loop.continueEntry();
                    builder.nop();
                }
            })).doesNotThrowAnyException();
        }
    }
}
