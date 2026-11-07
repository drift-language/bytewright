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

import java.lang.classfile.Label;


/**
 * A local variable declaration.
 *
 * @param name the name of the local variable.
 * @param slot the slot of the local variable.
 * @param type the {@link JvmType} of the local variable.
 * @param label the label belonging to the local variable.
 */
public record DeclaredLocal(
        String name,
        int slot,
        JvmType type,
        Label label
) { }
