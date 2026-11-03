/*******************************************************************************
 * Drift Programming Language
 * ByteWright Library
 *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)
 *
 * This source code is licensed under the Apache 2.0 License.
 * See the LICENSE file in the root directory for details.
 ******************************************************************************/

package dev.driftlang.bytewright.core;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.classfile.ClassFile;
import java.lang.constant.ClassDesc;
import java.lang.invoke.MethodHandles;
import java.util.Optional;

import static java.lang.constant.ConstantDescs.CD_Object;
import static java.lang.constant.ConstantDescs.CD_String;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class JvmTypeTest {

    @Nested
    class IsArray {

        @Test
        void trueForArrays_falseOtherwise() {
            // GIVEN
            JvmType array = JvmType.arrayOf(JvmType.INT);

            // WHEN / THEN
            assertSoftly(softly -> {
                softly.assertThat(array.isArray()).isTrue();
                softly.assertThat(JvmType.INT.isArray()).isFalse();
                softly.assertThat(JvmType.OBJECT.isArray()).isFalse();
            });
        }
    }


    @Nested
    class IsPrimitive {

        @Test
        void trueForPrimitives_falseForReferencesAndArrays() {
            // GIVEN
            JvmType array = JvmType.arrayOf(JvmType.INT);

            // WHEN / THEN
            assertSoftly(softly -> {
                softly.assertThat(JvmType.INT.isPrimitive()).isTrue();
                softly.assertThat(JvmType.OBJECT.isPrimitive()).isFalse();
                softly.assertThat(array.isPrimitive()).isFalse();
            });
        }
    }


    @Nested
    class ComponentType {

        @Test
        void ofArray_returnsComponent() {
            // GIVEN
            JvmType array = JvmType.arrayOf(JvmType.INT);

            // WHEN
            Optional<JvmType> component = array.componentType();

            // THEN
            assertThat(component).contains(JvmType.INT);
        }

        @Test
        void ofNonArray_returnsEmpty() {
            // GIVEN / WHEN
            Optional<JvmType> component = JvmType.OBJECT.componentType();

            // THEN
            assertThat(component).isEmpty();
        }

        @Test
        void ofTwoDimensionalArray_returnsOneDimensionalArray() {
            // GIVEN
            JvmType matrix = JvmType.arrayOf(JvmType.INT, 2);

            // WHEN
            Optional<JvmType> component = matrix.componentType();

            // THEN
            assertThat(component).contains(JvmType.arrayOf(JvmType.INT));
        }
    }


    @Nested
    class Of {

        @Test
        void fromClass_wrapsItsDescriptor() {
            // GIVEN / WHEN
            Optional<JvmType> type = JvmType.of(String.class);

            // THEN
            assertThat(type).contains(new JvmType(CD_String));
        }

        @Test
        void fromHiddenClass_returnsEmpty() throws IllegalAccessException {
            // GIVEN a hidden class, which has no nominal descriptor
            byte[] bytes = ClassFile.of().build(
                    ClassDesc.of("dev.driftlang.bytewright.core.GeneratedHiddenHost"),
                    classBuilder -> classBuilder.withSuperclass(CD_Object));

            Class<?> hidden = MethodHandles.lookup()
                    .defineHiddenClass(bytes, false)
                    .lookupClass();

            // WHEN
            Optional<JvmType> type = JvmType.of(hidden);

            // THEN
            assertThat(type).isEmpty();
        }

        @Test
        void fromClassDesc_wrapsIt() {
            // GIVEN / WHEN
            JvmType type = JvmType.of(CD_String);

            // THEN
            assertThat(type.descriptor()).isEqualTo(CD_String);
        }
    }


    @Nested
    class ArrayOf {

        @Test
        void oneDimension() {
            // GIVEN / WHEN
            JvmType array = JvmType.arrayOf(JvmType.INT);

            // THEN
            assertThat(array.descriptor().descriptorString()).isEqualTo("[I");
        }

        @Test
        void givenDimensions() {
            // GIVEN / WHEN
            JvmType array = JvmType.arrayOf(JvmType.OBJECT, 3);

            // THEN
            assertThat(array.descriptor().descriptorString()).isEqualTo("[[[Ljava/lang/Object;");
        }
    }
}
