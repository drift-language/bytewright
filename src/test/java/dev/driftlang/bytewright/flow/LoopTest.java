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
import dev.driftlang.bytewright.scope.ScopeManager;
import dev.driftlang.bytewright.scope.exception.ScopeAlreadyClosedException;
import org.junit.jupiter.api.Test;

import java.lang.classfile.ClassFile;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.MethodModel;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.function.Consumer;

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
    private static MethodModel buildMethod(Consumer<CodeBuilder> body) {
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


    @Test
    void constructor_withNullParentScope_throwsNullPointerException() {
        // GIVEN a null parent scope
        // WHEN / THEN constructing a Loop with it throws
        assertThatThrownBy(() -> new Loop(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("parentScope is marked non-null but is null");
    }

    @Test
    void constructor_createsChildScopeOfParent() {
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

    @Test
    void isStarted_beforeStart_isFalse() {
        // GIVEN / WHEN
        final Loop[] loop = new Loop[1];

        buildMethod(builder -> {
            ScopeManager manager = new ScopeManager(builder);
            loop[0] = new Loop(manager.scope());
        });

        // THEN
        assertThat(loop[0].isStarted()).isFalse();
    }

    @Test
    void start_marksLoopAsStarted() {
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
    void start_calledTwice_throwsFlowAlreadyStartedException() {
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
    void start_afterClose_throwsScopeAlreadyClosedException() {
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

    @Test
    void continue_beforeStart_throwsFlowNotStartedException() {
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
    void continue_afterStart_doesNotThrow() {
        // GIVEN a started loop
        // WHEN continuing it
        // THEN no exception is thrown
        assertThatCode(() -> buildMethod(builder -> {
            ScopeManager manager = new ScopeManager(builder);
            Loop loop = new Loop(manager.scope());
            loop.start();

            loop.continue_();
        })).doesNotThrowAnyException();
    }

    @Test
    void continue_afterClose_throwsScopeAlreadyClosedException() {
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
    void break_beforeStart_throwsFlowNotStartedException() {
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
    void break_afterStart_doesNotThrow() {
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
    void break_afterClose_throwsScopeAlreadyClosedException() {
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

    @Test
    void close_withoutStart_doesNotThrow() {
        // GIVEN a loop that has never been started
        // WHEN closing it
        // THEN a FlowNotStartedException is thrown
        assertThatThrownBy(() -> buildMethod(builder -> {
            ScopeManager manager = new ScopeManager(builder);
            Loop loop = new Loop(manager.scope());

            loop.close();
        }))
                .isInstanceOf(FlowNotStartedException.class);
    }

    @Test
    void close_calledTwice_throwsScopeAlreadyClosedException() {
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
    void close_closesUnderlyingScope() {
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
    void fullLifecycle_startContinueBreakClose_doesNotThrow() {
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
}
