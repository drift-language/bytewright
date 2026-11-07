/*******************************************************************************
 * Drift Programming Language
 * Bytewright Library
 *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)
 *
 * This source code is licensed under the Apache 2.0 License.
 * See the LICENSE file in the root directory for details.
 ******************************************************************************/

package dev.driftlang.bytewright.core;


import java.lang.classfile.TypeKind;
import java.lang.constant.ClassDesc;
import java.util.Optional;

import static java.lang.constant.ConstantDescs.*;

/**
 * A JVM type, as seen by Bytewright: a thin, immutable wrapper around a
 * {@link ClassDesc}.
 * <p>
 * It covers primitives, {@code void}, reference types, and arrays. It is
 * the type vocabulary shared by the whole toolkit (for instance, the type of
 * a {@link Value}). Bytewright never inspects the consuming language's own
 * type system: callers map their types to {@code JvmType} instances.
 * <p>
 * Common primitive types and {@code Object} are available as constants;
 * any other type can be built with the {@code of} and {@code arrayOf}
 * factory methods.
 *
 * @param descriptor the {@link ClassDesc} of the type.
 *
 * @author Jonathan (GitHub: belicfr)
 */
public record JvmType(
        ClassDesc descriptor) {

    /** Represents the {@code byte} primitive type. */
    public static final JvmType BYTE    = new JvmType(CD_byte);

    /** Represents the {@code short} primitive type. */
    public static final JvmType SHORT   = new JvmType(CD_short);

    /** Represents the {@code int} primitive type. */
    public static final JvmType INT     = new JvmType(CD_int);

    /** Represents the {@code long} primitive type. */
    public static final JvmType LONG    = new JvmType(CD_long);

    /** Represents the {@code float} primitive type. */
    public static final JvmType FLOAT   = new JvmType(CD_float);

    /** Represents the {@code double} primitive type. */
    public static final JvmType DOUBLE  = new JvmType(CD_double);

    /** Represents the {@code char} primitive type. */
    public static final JvmType CHAR    = new JvmType(CD_char);

    /** Represents the {@code boolean} primitive type. */
    public static final JvmType BOOLEAN = new JvmType(CD_boolean);

    /** Represents {@code void}. */
    public static final JvmType VOID    = new JvmType(CD_void);

    /** Represents an {@code Object}. */
    public static final JvmType OBJECT  = new JvmType(CD_Object);


    /**
     * Returns the {@link TypeKind} of this type.
     *
     * @return the {@link TypeKind} of this type.
     */
    public TypeKind kind() {
        return TypeKind.fromDescriptor(descriptor().descriptorString());
    }

    /**
     * Returns if the type is an array.
     *
     * @return {@code true} if the type is an array, and {@code false} otherwise.
     */
    public boolean isArray() {
        return descriptor().isArray();
    }

    /**
     * Returns if the type is primary.
     *
     * @return {@code true} if the type is primary, and {@code false} otherwise.
     */
    public boolean isPrimitive() {
        return descriptor().isPrimitive();
    }

    /**
     * If the current type is an array, it returns the contained type, {@link
     * Optional#empty()} otherwise.
     *
     * @return the elements' type if it is an array, empty otherwise.
     */
    public Optional<JvmType> componentType() {
        return isArray()
                ? Optional.of(new JvmType(descriptor().componentType()))
                : Optional.empty();
    }

    /**
     * Returns the slot size of the type.
     *
     * @return the slot size of the type.
     */
    public int slotSize() {
        return kind().slotSize();
    }


    /**
     * Gets the type from a given class instance.
     *
     * @param clazz the class instance from which to get the ClassDesc.
     * @return an Optional containing the type if it exists.
     */
    public static Optional<JvmType> of(final Class<?> clazz) {
        return clazz.describeConstable()
                .map(JvmType::new);
    }

    /**
     * Gets the type from a given class descriptor string.
     *
     * @param descriptor the class descriptor string from which to get the
     *                   ClassDesc.
     * @return an Optional containing the type if it exists;
     */
    public static JvmType of(final ClassDesc descriptor) {
        return new JvmType(descriptor);
    }

    /**
     * Creates a wrapped type representing an array.
     *
     * @param component the type of the components in the array.
     * @return the wrapped type representing an array of the given
     *         type.
     */
    public static JvmType arrayOf(final JvmType component) {
        return new JvmType(component.descriptor().arrayType());
    }

    /**
     * Creates a wrapped type representing an array of the given
     * dimension.
     *
     * @param component the type of the components in the array.
     * @param dimensions the dimension of the array to create.
     * @return the wrapped type representing an array of the given
     *         type and dimension.
     */
    public static JvmType arrayOf(final JvmType component, final int dimensions) {
        return new JvmType(component.descriptor().arrayType(dimensions));
    }
}