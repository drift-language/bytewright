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

import lombok.NonNull;

import java.lang.classfile.TypeKind;
import java.lang.constant.ClassDesc;
import java.util.Optional;

import static java.lang.constant.ConstantDescs.*;

/**
 *
 *
 * @author Jonathan (GitHub: belicfr)
 */
public record JvmType(@NonNull ClassDesc descriptor) {

    public static final JvmType BYTE    = new JvmType(CD_byte);
    public static final JvmType SHORT   = new JvmType(CD_short);
    public static final JvmType INT     = new JvmType(CD_int);
    public static final JvmType LONG    = new JvmType(CD_long);
    public static final JvmType FLOAT   = new JvmType(CD_float);
    public static final JvmType DOUBLE  = new JvmType(CD_double);
    public static final JvmType CHAR    = new JvmType(CD_char);
    public static final JvmType BOOLEAN = new JvmType(CD_boolean);
    public static final JvmType VOID    = new JvmType(CD_void);
    public static final JvmType OBJECT  = new JvmType(CD_Object);


    public TypeKind kind() {
        return TypeKind.fromDescriptor(descriptor().descriptorString());
    }

    public boolean isArray() {
        return descriptor().isArray();
    }

    public boolean isPrimitive() {
        return descriptor().isPrimitive();
    }

    public Optional<JvmType> componentType() {
        return isArray()
                ? Optional.of(new JvmType(descriptor().componentType()))
                : Optional.empty();

    }

    public int slotSize() {
        return kind().slotSize();
    }


    /**
     * Gets the {@link JvmType} from a given class instance.
     *
     * @param clazz the class instance from which to get the ClassDesc.
     * @return an Optional containing the {@link JvmType} if it exists.
     */
    public static Optional<JvmType> of(@NonNull final Class<?> clazz) {
        return clazz.describeConstable()
                .map(JvmType::new);
    }

    /**
     * Gets the {@link JvmType} from a given class descriptor string.
     *
     * @param descriptor the class descriptor string from which to get the
     *                   ClassDesc.
     * @return an Optional containing the {@link JvmType} if it exists;
     */
    public static JvmType of(@NonNull final ClassDesc descriptor) {
        return new JvmType(descriptor);
    }

    /**
     * Creates a wrapped {@link JvmType} representing an array.
     *
     * @param component the type of the components in the array.
     * @return the wrapped {@link JvmType} representing an array of the given
     *         type.
     */
    public static JvmType arrayOf(@NonNull final JvmType component) {
        return new JvmType(component.descriptor().arrayType());
    }

    /**
     * Creates a wrapped {@link JvmType} representing an array of the given
     * dimension.
     *
     * @param component the type of the components in the array.
     * @param dimensions the dimension of the array to create.
     * @return the wrapped {@link JvmType} representing an array of the given
     *         type and dimension.
     */
    public static JvmType arrayOf(@NonNull final JvmType component, final int dimensions) {
        return new JvmType(component.descriptor().arrayType(dimensions));
    }
}