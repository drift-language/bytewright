/*******************************************************************************
 * Drift Programming Language
 * Bytewright Library
 *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)
 *
 * This source code is licensed under the Apache 2.0 License.
 * See the LICENSE file in the root directory for details.
 ******************************************************************************/

package dev.driftlang.bytewright.scope;

import dev.driftlang.bytewright.core.JvmType;
import dev.driftlang.bytewright.core.Value;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.classfile.ClassFile;
import java.lang.classfile.TypeKind;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.stream.Stream;

import static java.lang.constant.ConstantDescs.CD_Object;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link Local#store} and {@link Local#load} are checked by running them:
 * each test generates a real {@code static T test(T x)} method that stores
 * {@code x} into the local, loads it back and returns it.
 */
class LocalTest {

    private static final ClassDesc HOST = ClassDesc.of("dev.driftlang.bytewright.scope.GeneratedLocalHost");


    @ParameterizedTest(name = "{0}")
    @MethodSource("roundTripCases")
    void storeThenLoad_roundTripsValue(final JvmType type, final Class<?> javaType, final Object input) throws Throwable {
        // GIVEN a local placed right after the parameter (2 slots for long/double)
        Local local = new Local(type.slotSize(), type);
        MethodHandle test = compileRoundTrip(local, javaType);

        // WHEN
        Object result = test.invoke(input);

        // THEN
        assertThat(result).isEqualTo(input);
    }

    @Test
    void load_valueHasLocalType() {
        // GIVEN
        Local local = new Local(0, JvmType.LONG);

        // WHEN
        Value value = local.load();

        // THEN
        assertThat(value.type()).isEqualTo(JvmType.LONG);
    }


    static Stream<Arguments> roundTripCases() {
        return Stream.of(
                Arguments.of(JvmType.INT, int.class, 42),
                Arguments.of(JvmType.LONG, long.class, 42L),
                Arguments.of(JvmType.DOUBLE, double.class, 4.2),
                Arguments.of(JvmType.OBJECT, Object.class, "hello"));
    }

    /**
     * Generates and loads a {@code test(x)} method that stores {@code x} into
     * {@code local}, loads it back and returns it.
     */
    private static MethodHandle compileRoundTrip(final Local local, final Class<?> javaType) throws Exception {
        final ClassDesc descriptor = local.type().descriptor();
        final TypeKind kind = local.type().kind();

        byte[] bytes = ClassFile.of().build(HOST, classBuilder -> classBuilder
                .withSuperclass(CD_Object)
                .withMethodBody(
                        "test",
                        MethodTypeDesc.of(descriptor, descriptor),
                        ClassFile.ACC_PUBLIC | ClassFile.ACC_STATIC,
                        codeBuilder -> {
                            codeBuilder.loadLocal(kind, 0);
                            local.store(codeBuilder);
                            local.load().emit(codeBuilder);
                            codeBuilder.return_(kind);
                        }));

        MethodHandles.Lookup lookup = MethodHandles.lookup().defineHiddenClass(bytes, true);

        return lookup.findStatic(lookup.lookupClass(), "test", MethodType.methodType(javaType, javaType));
    }
}
