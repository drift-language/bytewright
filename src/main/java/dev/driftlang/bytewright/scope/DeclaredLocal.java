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

import java.lang.classfile.Label;


/**
 *
 *
 * @param name
 * @param slot
 * @param type
 * @param label
 */
public record DeclaredLocal(
        String name,
        int slot,
        JvmType type,
        Label label
) { }
