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

import java.lang.classfile.CodeBuilder;


/**
 *
 *
 * @author Jonathan (GitHub: belicfr)
 */
public interface Value {

    /**
     *
     *
     * @return
     */
    JvmType type();

    /**
     *
     *
     * @param cb
     */
    void emit(@NonNull final CodeBuilder cb);
}
