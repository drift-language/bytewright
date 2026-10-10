/*******************************************************************************
 * Drift Programming Language
 * ByteWright Library
 *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)
 *
 * This source code is licensed under the Apache 2.0 License.
 * See the LICENSE file in the root directory for details.
 ******************************************************************************/

package dev.driftlang.bytewright.scope;

import dev.driftlang.bytewright.core.JvmType;
import dev.driftlang.bytewright.scope.exception.AlreadyBoundNameException;
import dev.driftlang.bytewright.scope.exception.ScopeAlreadyClosedException;
import dev.driftlang.bytewright.scope.exception.UnexpectedScopeByManagerException;
import org.junit.jupiter.api.Test;

import java.lang.classfile.Attributes;
import java.lang.classfile.ClassFile;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.MethodModel;
import java.lang.classfile.attribute.LocalVariableInfo;
import java.lang.classfile.attribute.LocalVariableTableAttribute;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static java.lang.constant.ConstantDescs.CD_void;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * {@link ScopeManager} can only be exercised against a real {@link CodeBuilder}
 * (labels, the local variable table, etc. are stateful and tied to an actual
 * method body being built), so every test here drives it through a throwaway
 * method generated with {@link ClassFile}.
 */
class ScopeManagerTest {

    private static final ClassDesc HOST = ClassDesc.of("dev.driftlang.bytewright.scope.GeneratedTestHost");

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

    private static List<LocalVariableInfo> localVariableTableOf(MethodModel method) {
        return method.code()
                .flatMap(code -> code.findAttribute(Attributes.localVariableTable()))
                .map(LocalVariableTableAttribute::localVariables)
                .orElse(List.of());
    }


    @Test
    void declare_assignsSequentialSlotsAccountingForSlotSize() {
        // GIVEN
        final int[] slots = new int[3];

        // WHEN
        buildMethod(builder -> {
            ScopeManager manager = new ScopeManager(builder);
            try (ScopeManager.Scope scope = manager.scope()) {
                slots[0] = scope.declare("a", JvmType.INT).slot();
                slots[1] = scope.declare("b", JvmType.LONG).slot();
                slots[2] = scope.declare("c", JvmType.INT).slot();
            }
        });

        // THEN
        assertSoftly(softly -> {
            softly.assertThat(slots[0]).isEqualTo(0);
            softly.assertThat(slots[1]).isEqualTo(1); // 'a' (int) takes 1 slot
            softly.assertThat(slots[2]).isEqualTo(3); // 'b' (long) takes 2 slots
        });
    }

    @Test
    void declare_duplicateNameInSameScope_throws() {
        // GIVEN a scope with a local already declared
        // WHEN declaring another local with the same name
        // THEN an AlreadyBoundNameException is thrown
        assertThatThrownBy(() -> buildMethod(builder -> {
            ScopeManager manager = new ScopeManager(builder);
            try (ScopeManager.Scope scope = manager.scope()) {
                scope.declare("x", JvmType.INT);
                scope.declare("x", JvmType.INT);
            }
        })).isInstanceOf(AlreadyBoundNameException.class);
    }

    @Test
    void declare_afterClose_throwsScopeAlreadyClosedException() {
        // GIVEN a scope that has already been closed
        // WHEN declaring a local on it
        // THEN a ScopeAlreadyClosedException is thrown
        assertThatThrownBy(() -> buildMethod(builder -> {
            ScopeManager manager = new ScopeManager(builder);
            ScopeManager.Scope scope = manager.scope();
            scope.close();

            scope.declare("x", JvmType.INT);
        })).isInstanceOf(ScopeAlreadyClosedException.class);
    }

    @Test
    void close_calledTwice_throwsScopeAlreadyClosedException() {
        // GIVEN a scope that has already been closed
        // WHEN closing it a second time
        // THEN a ScopeAlreadyClosedException is thrown
        assertThatThrownBy(() -> buildMethod(builder -> {
            ScopeManager manager = new ScopeManager(builder);
            ScopeManager.Scope scope = manager.scope();
            scope.close();

            scope.close();
        })).isInstanceOf(ScopeAlreadyClosedException.class);
    }

    @Test
    void declare_allowsShadowingNameFromParentScope() {
        // GIVEN a parent scope with a local declared
        // WHEN a child scope declares a local with the same name
        // THEN no exception is thrown
        assertThatCode(() -> buildMethod(builder -> {
            ScopeManager manager = new ScopeManager(builder);
            try (ScopeManager.Scope parent = manager.scope()) {
                parent.declare("x", JvmType.INT);

                try (ScopeManager.Scope child = manager.scope(parent)) {
                    child.declare("x", JvmType.INT);
                }
            }
        })).doesNotThrowAnyException();
    }

    @Test
    void close_reusesFreedSlotForNextScope() {
        // GIVEN
        final int[] slots = new int[2];

        // WHEN
        buildMethod(builder -> {
            ScopeManager manager = new ScopeManager(builder);

            try (ScopeManager.Scope first = manager.scope()) {
                slots[0] = first.declare("a", JvmType.INT).slot();
            }

            try (ScopeManager.Scope second = manager.scope()) {
                slots[1] = second.declare("b", JvmType.INT).slot();
            }
        });

        // THEN
        assertSoftly(softly -> {
            softly.assertThat(slots[0]).isEqualTo(0);
            softly.assertThat(slots[1]).isEqualTo(0); // slot freed by closing 'first' is reused
        });
    }

    @Test
    void resolve_findsLocalDeclaredInCurrentScope() {
        // GIVEN
        final Optional<DeclaredLocal>[] found = new Optional[1];

        // WHEN
        buildMethod(builder -> {
            ScopeManager manager = new ScopeManager(builder);
            try (ScopeManager.Scope scope = manager.scope()) {
                scope.declare("x", JvmType.INT);
                found[0] = scope.resolve("x");
            }
        });

        // THEN
        assertSoftly(softly -> softly.assertThat(found[0])
                .hasValueSatisfying(local -> softly.assertThat(local.name()).isEqualTo("x")));
    }

    @Test
    void resolve_findsLocalDeclaredInParentScope() {
        // GIVEN
        final Optional<DeclaredLocal>[] found = new Optional[1];

        // WHEN
        buildMethod(builder -> {
            ScopeManager manager = new ScopeManager(builder);
            try (ScopeManager.Scope parent = manager.scope()) {
                parent.declare("x", JvmType.INT);

                try (ScopeManager.Scope child = manager.scope(parent)) {
                    found[0] = child.resolve("x");
                }
            }
        });

        // THEN
        assertSoftly(softly -> softly.assertThat(found[0])
                .hasValueSatisfying(local -> softly.assertThat(local.name()).isEqualTo("x")));
    }

    @Test
    void resolve_returnsEmpty_whenNameIsNotDeclaredAnywhere() {
        // GIVEN
        final Optional<DeclaredLocal>[] found = new Optional[1];

        // WHEN
        buildMethod(builder -> {
            ScopeManager manager = new ScopeManager(builder);
            try (ScopeManager.Scope scope = manager.scope()) {
                found[0] = scope.resolve("missing");
            }
        });

        // THEN
        assertThat(found[0]).isEmpty();
    }

    @Test
    void isNameBoundInCurrentScope_ignoresParentDeclarations() {
        // GIVEN
        final boolean[] boundInChild = new boolean[1];

        // WHEN
        buildMethod(builder -> {
            ScopeManager manager = new ScopeManager(builder);
            try (ScopeManager.Scope parent = manager.scope()) {
                parent.declare("x", JvmType.INT);

                try (ScopeManager.Scope child = manager.scope(parent)) {
                    boundInChild[0] = child.isNameBoundInCurrentScope("x");
                }
            }
        });

        // THEN
        assertThat(boundInChild[0]).isFalse();
    }

    @Test
    void hasParent_reflectsWhetherAParentWasProvided() {
        // GIVEN
        final boolean[] rootHasParent = new boolean[1];
        final boolean[] childHasParent = new boolean[1];

        // WHEN
        buildMethod(builder -> {
            ScopeManager manager = new ScopeManager(builder);
            try (ScopeManager.Scope root = manager.scope()) {
                rootHasParent[0] = root.hasParent();

                try (ScopeManager.Scope child = manager.scope(root)) {
                    childHasParent[0] = child.hasParent();
                }
            }
        });

        // THEN
        assertSoftly(softly -> {
            softly.assertThat(rootHasParent[0]).isFalse();
            softly.assertThat(childHasParent[0]).isTrue();
        });
    }

    @Test
    void scope_withParentFromAnotherManager_throws() {
        // GIVEN a scope owned by a different ScopeManager
        final ScopeManager.Scope[] scopeFromOtherManager = new ScopeManager.Scope[1];

        buildMethod(builder -> {
            ScopeManager otherManager = new ScopeManager(builder);
            scopeFromOtherManager[0] = otherManager.scope();
        });

        // WHEN / THEN using it as a parent on another manager throws
        buildMethod(builder -> {
            ScopeManager manager = new ScopeManager(builder);
            assertThatThrownBy(() -> manager.scope(scopeFromOtherManager[0]))
                    .isInstanceOf(UnexpectedScopeByManagerException.class);
        });
    }

    @Test
    void close_emitsLocalVariableTableEntryWithNameSlotAndType() {
        // GIVEN / WHEN
        MethodModel method = buildMethod(builder -> {
            ScopeManager manager = new ScopeManager(builder);
            try (ScopeManager.Scope scope = manager.scope()) {
                scope.declare("x", JvmType.INT);
            }
        });

        // THEN
        List<LocalVariableInfo> table = localVariableTableOf(method);
        assertThat(table).hasSize(1);

        LocalVariableInfo entry = table.getFirst();
        assertSoftly(softly -> {
            softly.assertThat(entry.name().stringValue()).isEqualTo("x");
            softly.assertThat(entry.slot()).isEqualTo(0);
            softly.assertThat(entry.typeSymbol()).isEqualTo(JvmType.INT.descriptor());
        });
    }

    @Test
    void close_emitsOneLocalVariableTableEntryPerDeclaredLocal() {
        // GIVEN / WHEN
        MethodModel method = buildMethod(builder -> {
            ScopeManager manager = new ScopeManager(builder);
            try (ScopeManager.Scope scope = manager.scope()) {
                scope.declare("a", JvmType.INT);
                scope.declare("b", JvmType.LONG);
            }
        });

        // THEN
        List<LocalVariableInfo> table = localVariableTableOf(method);
        assertThat(table).hasSize(2);

        assertSoftly(softly -> {
            softly.assertThat(table)
                    .anyMatch(info -> info.name().stringValue().equals("a") && info.slot() == 0);
            softly.assertThat(table)
                    .anyMatch(info -> info.name().stringValue().equals("b") && info.slot() == 1);
        });
    }
}
