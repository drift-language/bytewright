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


/**
 * {@link Flow} is abstract and only meant to be extended by concrete
 * constructs like {@link Loop}/{@link Switch}, so a minimal {@link TestFlow}
 * subclass (adding no behavior of its own) is used here to exercise its
 * shared logic in isolation, against a real {@link CodeBuilder} the same way
 * {@link LoopTest} does.
 */
class FlowTest {

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
        // WHEN / THEN constructing a Flow with it throws
        assertThatThrownBy(() -> new TestFlow(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("parentScope is marked non-null but is null");
    }

    @Test
    void constructor_createsChildScopeOfParent() {
        // GIVEN
        final ScopeManager.Scope[] parent = new ScopeManager.Scope[1];
        final TestFlow[] flow = new TestFlow[1];

        // WHEN
        buildMethod(builder -> {
            ScopeManager manager = new ScopeManager(builder);
            parent[0] = manager.scope();
            flow[0] = new TestFlow(parent[0]);
        });

        // THEN
        assertThat(flow[0].getScope().getParent()).isSameAs(parent[0]);
    }

    @Test
    void isStarted_beforeStart_isFalse() {
        // GIVEN / WHEN
        final TestFlow[] flow = new TestFlow[1];

        buildMethod(builder -> {
            ScopeManager manager = new ScopeManager(builder);
            flow[0] = new TestFlow(manager.scope());
        });

        // THEN
        assertThat(flow[0].isStarted()).isFalse();
    }

    @Test
    void start_marksAsStarted() {
        // GIVEN
        final TestFlow[] flow = new TestFlow[1];

        // WHEN
        buildMethod(builder -> {
            ScopeManager manager = new ScopeManager(builder);
            flow[0] = new TestFlow(manager.scope());
            flow[0].start();
        });

        // THEN
        assertThat(flow[0].isStarted()).isTrue();
    }

    @Test
    void start_calledTwice_throwsFlowAlreadyStartedException() {
        // GIVEN a flow that has already been started
        // WHEN starting it a second time
        // THEN a FlowAlreadyStartedException is thrown
        assertThatThrownBy(() -> buildMethod(builder -> {
            ScopeManager manager = new ScopeManager(builder);
            TestFlow flow = new TestFlow(manager.scope());
            flow.start();

            flow.start();
        })).isInstanceOf(FlowAlreadyStartedException.class);
    }

    @Test
    void requireStarted_beforeStart_throwsFlowNotStartedException() {
        // GIVEN a flow that has not been started
        // WHEN requiring it to be started
        // THEN a FlowNotStartedException is thrown
        assertThatThrownBy(() -> buildMethod(builder -> {
            ScopeManager manager = new ScopeManager(builder);
            TestFlow flow = new TestFlow(manager.scope());

            flow.requireStarted();
        })).isInstanceOf(FlowNotStartedException.class);
    }

    @Test
    void requireStarted_afterStart_doesNotThrow() {
        // GIVEN a started flow
        // WHEN requiring it to be started
        // THEN no exception is thrown
        assertThatCode(() -> buildMethod(builder -> {
            ScopeManager manager = new ScopeManager(builder);
            TestFlow flow = new TestFlow(manager.scope());
            flow.start();

            flow.requireStarted();
        })).doesNotThrowAnyException();
    }

    @Test
    void requireNotStarted_beforeStart_doesNotThrow() {
        // GIVEN a flow that has not been started
        // WHEN requiring it to not be started
        // THEN no exception is thrown
        assertThatCode(() -> buildMethod(builder -> {
            ScopeManager manager = new ScopeManager(builder);
            TestFlow flow = new TestFlow(manager.scope());

            flow.requireNotStarted();
        })).doesNotThrowAnyException();
    }

    @Test
    void requireNotStarted_afterStart_throwsFlowAlreadyStartedException() {
        // GIVEN a started flow
        // WHEN requiring it to not be started
        // THEN a FlowAlreadyStartedException is thrown
        assertThatThrownBy(() -> buildMethod(builder -> {
            ScopeManager manager = new ScopeManager(builder);
            TestFlow flow = new TestFlow(manager.scope());
            flow.start();

            flow.requireNotStarted();
        })).isInstanceOf(FlowAlreadyStartedException.class);
    }

    @Test
    void close_withoutStart_doesNotThrow() {
        // GIVEN a flow that has never been started
        // WHEN closing it
        // THEN no exception is thrown
        assertThatCode(() -> buildMethod(builder -> {
            ScopeManager manager = new ScopeManager(builder);
            TestFlow flow = new TestFlow(manager.scope());

            flow.close();
        })).doesNotThrowAnyException();
    }

    @Test
    void close_closesUnderlyingScope() {
        // GIVEN
        final TestFlow[] flow = new TestFlow[1];

        // WHEN
        buildMethod(builder -> {
            ScopeManager manager = new ScopeManager(builder);
            flow[0] = new TestFlow(manager.scope());

            flow[0].close();
        });

        // THEN
        assertThat(flow[0].getScope().isClosed()).isTrue();
    }

    @Test
    void close_calledTwice_throwsScopeAlreadyClosedException() {
        // GIVEN a flow that has already been closed
        // WHEN closing it a second time
        // THEN a ScopeAlreadyClosedException is thrown
        assertThatThrownBy(() -> buildMethod(builder -> {
            ScopeManager manager = new ScopeManager(builder);
            TestFlow flow = new TestFlow(manager.scope());
            flow.close();

            flow.close();
        })).isInstanceOf(ScopeAlreadyClosedException.class);
    }


    private static final class TestFlow extends Flow {
        private TestFlow(final ScopeManager.Scope parentScope) {
            super(parentScope);
        }
    }
}
