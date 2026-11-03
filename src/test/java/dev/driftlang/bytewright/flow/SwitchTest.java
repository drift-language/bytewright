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
import dev.driftlang.bytewright.flow.exception.UnregisteredSwitchCaseByValueException;
import dev.driftlang.bytewright.scope.ScopeManager;
import dev.driftlang.bytewright.scope.exception.ScopeAlreadyClosedException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.classfile.ClassFile;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.Label;
import java.lang.classfile.MethodModel;
import java.lang.classfile.instruction.LookupSwitchInstruction;
import java.lang.classfile.instruction.TableSwitchInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import static java.lang.constant.ConstantDescs.CD_void;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * {@link Switch} can only be exercised against a real {@link CodeBuilder}
 * (labels, gotos and the dispatch instruction itself are stateful and tied to
 * an actual method body being built), so every test here drives it through a
 * throwaway method generated with {@link ClassFile}, the same way
 * {@link LoopTest} does.
 */
class SwitchTest {

    private static final ClassDesc HOST = ClassDesc.of("dev.driftlang.bytewright.flow.GeneratedTestHost");


    @Nested
    class Select {

        @Test
        void withDenseCaseValues_returnsTable() {
            // GIVEN / WHEN / THEN case values 1, 2, 3 are dense enough for a table
            assertThat(Switch.Kind.select(1, 3, 3)).isEqualTo(Switch.Kind.TABLE);
        }

        @Test
        void withSparseCaseValues_returnsLookup() {
            // GIVEN / WHEN / THEN case values 1 and 1000 are far too sparse for a table
            assertThat(Switch.Kind.select(1, 1000, 2)).isEqualTo(Switch.Kind.LOOKUP);
        }

        @Test
        void withZeroCases_returnsLookup() {
            // GIVEN / WHEN / THEN an empty switch falls back to lookup
            assertThat(Switch.Kind.select(0, 0, 0)).isEqualTo(Switch.Kind.LOOKUP);
        }
    }


    @Nested
    class Constructor {

        @Test
        void createsChildScopeOfParent() {
            // GIVEN
            final ScopeManager.Scope[] parent = new ScopeManager.Scope[1];
            final Switch[] switch_ = new Switch[1];

            // WHEN
            buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);
                parent[0] = manager.scope();
                switch_[0] = new Switch(parent[0], Map.of(), builder.newLabel());
            });

            // THEN
            assertThat(switch_[0].getScope().getParent()).isSameAs(parent[0]);
        }
    }


    @Nested
    class ResolveLabel {

        @Test
        void withRegisteredValue_returnsItsLabel() {
            // GIVEN
            final Label[] caseLabel = new Label[1];
            final Switch[] switch_ = new Switch[1];

            // WHEN
            buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);
                caseLabel[0] = builder.newLabel();
                Map<Integer, Label> caseLabels = new LinkedHashMap<>();
                caseLabels.put(5, caseLabel[0]);

                switch_[0] = new Switch(manager.scope(), caseLabels, builder.newLabel());
            });

            // THEN
            assertThat(switch_[0].resolveLabel(5)).isSameAs(caseLabel[0]);
        }

        @Test
        void withUnregisteredValue_returnsDefaultLabel() {
            // GIVEN
            final Label[] defaultLabel = new Label[1];
            final Switch[] switch_ = new Switch[1];

            // WHEN
            buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);
                defaultLabel[0] = builder.newLabel();

                switch_[0] = new Switch(manager.scope(), Map.of(5, builder.newLabel()), defaultLabel[0]);
            });

            // THEN
            assertThat(switch_[0].resolveLabel(999)).isSameAs(defaultLabel[0]);
        }
    }


    @Nested
    class Compute {

        @Test
        void marksAsStarted() {
            // GIVEN
            final Switch[] switch_ = new Switch[1];

            // WHEN
            buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);
                switch_[0] = buildAndCompleteSwitch(manager.scope(), 1, 2);
            });

            // THEN
            assertThat(switch_[0].isStarted()).isTrue();
        }

        @Test
        void calledTwice_throwsFlowAlreadyStartedException() {
            // GIVEN a switch that has already computed its dispatch instruction
            // WHEN computing it a second time
            // THEN a FlowAlreadyStartedException is thrown
            assertThatThrownBy(() -> buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);
                Switch switch_ = new Switch(manager.scope(), Map.of(1, builder.newLabel()), builder.newLabel());
                switch_.compute();

                switch_.compute();
            })).isInstanceOf(FlowAlreadyStartedException.class);
        }

        @Test
        void afterClose_throwsScopeAlreadyClosedException() {
            // GIVEN a switch that has already been closed
            // WHEN computing its dispatch instruction
            // THEN a ScopeAlreadyClosedException is thrown
            assertThatThrownBy(() -> buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);
                Switch switch_ = new Switch(manager.scope(), Map.of(1, builder.newLabel()), builder.newLabel());
                switch_.start();
                switch_.close();

                switch_.compute();
            })).isInstanceOf(ScopeAlreadyClosedException.class);
        }

        @Test
        void withDenseCaseValues_emitsTableSwitch() {
            // GIVEN / WHEN a switch with tightly packed case values is completed
            MethodModel method = buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);
                buildAndCompleteSwitch(manager.scope(), 1, 2, 3);
            });

            // THEN the heuristic picked tableswitch, and only tableswitch
            assertSoftly(softly -> {
                softly.assertThat(emitsTableSwitch(method)).isTrue();
                softly.assertThat(emitsLookupSwitch(method)).isFalse();
            });
        }

        @Test
        void withSparseCaseValues_emitsLookupSwitch() {
            // GIVEN / WHEN a switch with far-apart case values is completed
            MethodModel method = buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);
                buildAndCompleteSwitch(manager.scope(), 1, 1000);
            });

            // THEN the heuristic picked lookupswitch, and only lookupswitch
            assertSoftly(softly -> {
                softly.assertThat(emitsLookupSwitch(method)).isTrue();
                softly.assertThat(emitsTableSwitch(method)).isFalse();
            });
        }
    }


    @Nested
    class BodyEntry {

        @Test
        void beforeCompute_throwsFlowNotStartedException() {
            // GIVEN a switch whose dispatch instruction hasn't been computed yet
            // WHEN entering a case body
            // THEN a FlowNotStartedException is thrown
            assertThatThrownBy(() -> buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);
                Switch switch_ = new Switch(manager.scope(), Map.of(1, builder.newLabel()), builder.newLabel());

                switch_.bodyEntry(1);
            })).isInstanceOf(FlowNotStartedException.class);
        }

        @Test
        void withUnregisteredValue_throwsUnregisteredSwitchCaseValueException() {
            // GIVEN a computed switch
            // WHEN entering a case body for a value with no registered case
            // THEN an UnregisteredSwitchCaseValueException is thrown
            assertThatThrownBy(() -> buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);
                Switch switch_ = new Switch(manager.scope(), Map.of(1, builder.newLabel()), builder.newLabel());
                switch_.compute();

                switch_.bodyEntry(999);
            })).isInstanceOf(UnregisteredSwitchCaseByValueException.class);
        }

        @Test
        void afterClose_throwsScopeAlreadyClosedException() {
            // GIVEN a switch that has already been closed
            // WHEN entering a case body
            // THEN a ScopeAlreadyClosedException is thrown
            assertThatThrownBy(() -> buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);
                Switch switch_ = new Switch(manager.scope(), Map.of(1, builder.newLabel()), builder.newLabel());
                switch_.start();
                switch_.close();

                switch_.bodyEntry(1);
            })).isInstanceOf(ScopeAlreadyClosedException.class);
        }
    }


    @Nested
    class DefaultEntry {

        @Test
        void beforeCompute_throwsFlowNotStartedException() {
            // GIVEN a switch whose dispatch instruction hasn't been computed yet
            // WHEN entering the default body
            // THEN a FlowNotStartedException is thrown
            assertThatThrownBy(() -> buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);
                Switch switch_ = new Switch(manager.scope(), Map.of(1, builder.newLabel()), builder.newLabel());

                switch_.defaultEntry();
            })).isInstanceOf(FlowNotStartedException.class);
        }

        @Test
        void afterClose_throwsScopeAlreadyClosedException() {
            // GIVEN a switch that has already been closed
            // WHEN entering the default body
            // THEN a ScopeAlreadyClosedException is thrown
            assertThatThrownBy(() -> buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);
                Switch switch_ = new Switch(manager.scope(), Map.of(1, builder.newLabel()), builder.newLabel());
                switch_.start();
                switch_.close();

                switch_.defaultEntry();
            })).isInstanceOf(ScopeAlreadyClosedException.class);
        }
    }


    @Nested
    class Break {

        @Test
        void beforeCompute_throwsFlowNotStartedException() {
            // GIVEN a switch whose dispatch instruction hasn't been computed yet
            // WHEN breaking it
            // THEN a FlowNotStartedException is thrown
            assertThatThrownBy(() -> buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);
                Switch switch_ = new Switch(manager.scope(), Map.of(1, builder.newLabel()), builder.newLabel());

                switch_.break_();
            })).isInstanceOf(FlowNotStartedException.class);
        }

        @Test
        void afterCompute_doesNotThrow() {
            // GIVEN a computed switch
            // WHEN breaking from within a case body, then completing the switch
            // THEN no exception is thrown
            assertThatCode(() -> buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);
                ScopeManager.Scope parentScope = manager.scope();

                Label caseLabel = builder.newLabel();
                Label defaultLabel = builder.newLabel();
                Switch switch_ = new Switch(parentScope, Map.of(1, caseLabel), defaultLabel);

                builder.loadConstant(0);
                switch_.compute();
                switch_.bodyEntry(1);
                switch_.break_();
                switch_.defaultEntry();
                switch_.close();
            })).doesNotThrowAnyException();
        }

        @Test
        void afterClose_throwsScopeAlreadyClosedException() {
            // GIVEN a switch that has already been closed
            // WHEN breaking it
            // THEN a ScopeAlreadyClosedException is thrown
            assertThatThrownBy(() -> buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);
                Switch switch_ = new Switch(manager.scope(), Map.of(1, builder.newLabel()), builder.newLabel());
                switch_.start();
                switch_.close();

                switch_.break_();
            })).isInstanceOf(ScopeAlreadyClosedException.class);
        }
    }


    @Nested
    class Close {

        @Test
        void withoutCompute_doesNotThrow() {
            // GIVEN a switch that has never computed its dispatch instruction
            // WHEN closing it
            // THEN no exception is thrown
            assertThatCode(() -> buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);
                Switch switch_ = new Switch(manager.scope(), Map.of(1, builder.newLabel()), builder.newLabel());
                switch_.start();

                switch_.close();
            })).doesNotThrowAnyException();
        }

        @Test
        void calledTwice_throwsScopeAlreadyClosedException() {
            // GIVEN a switch that has already been closed
            // WHEN closing it a second time
            // THEN a ScopeAlreadyClosedException is thrown
            assertThatThrownBy(() -> buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);
                Switch switch_ = new Switch(manager.scope(), Map.of(1, builder.newLabel()), builder.newLabel());
                switch_.start();
                switch_.close();

                switch_.close();
            })).isInstanceOf(ScopeAlreadyClosedException.class);
        }

        @Test
        void closesUnderlyingScope() {
            // GIVEN
            final Switch[] switch_ = new Switch[1];

            // WHEN
            buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);
                switch_[0] = buildAndCompleteSwitch(manager.scope(), 1, 2);
            });

            // THEN
            assertThat(switch_[0].getScope().isClosed()).isTrue();
        }
    }


    @Nested
    class FullLifecycle {

        @Test
        void computeBodyEntriesClose_doesNotThrow() {
            // GIVEN / WHEN / THEN a switch with several cases taken through its
            // whole lifecycle builds valid bytecode without throwing
            assertThatCode(() -> buildMethod(builder -> {
                ScopeManager manager = new ScopeManager(builder);
                buildAndCompleteSwitch(manager.scope(), 1, 2, 3);
            })).doesNotThrowAnyException();
        }
    }


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
     * Builds a {@link Switch} for the given case values, drives it through its
     * whole lifecycle (dispatch, every case body, the default body, and
     * close), and returns it.
     */
    private static Switch buildAndCompleteSwitch(final ScopeManager.Scope parentScope, final int... values) {
        final CodeBuilder builder = parentScope.getBuilder();
        final Map<Integer, Label> caseLabels = new LinkedHashMap<>();
        for (final int value : values) {
            caseLabels.put(value, builder.newLabel());
        }
        final Label defaultLabel = builder.newLabel();

        final Switch switch_ = new Switch(parentScope, caseLabels, defaultLabel);
        builder.loadConstant(0);
        switch_.compute();
        for (final int value : values) {
            switch_.bodyEntry(value);
        }
        switch_.defaultEntry();
        switch_.close();

        return switch_;
    }

    /**
     * Returns whether {@code method} contains a {@code tableswitch}
     * instruction.
     */
    private static boolean emitsTableSwitch(final MethodModel method) {
        return method.code().orElseThrow()
                .elementStream()
                .anyMatch(TableSwitchInstruction.class::isInstance);
    }

    /**
     * Returns whether {@code method} contains a {@code lookupswitch}
     * instruction.
     */
    private static boolean emitsLookupSwitch(final MethodModel method) {
        return method.code().orElseThrow()
                .elementStream()
                .anyMatch(LookupSwitchInstruction.class::isInstance);
    }
}
