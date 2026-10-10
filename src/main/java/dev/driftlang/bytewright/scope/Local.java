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
import dev.driftlang.bytewright.core.Value;

import java.lang.classfile.CodeBuilder;


/**
 * A local variable, defined at a given slot index, with a specific type.
 *
 * @param slot slot index related to the declared local variable, in the owner
 *             scope.
 * @param type the type of the local variable.
 */
public record Local(
        int slot,
        JvmType type
) {

    /**
     * Emits the local storage of this local variable via the code builder.
     *
     * @param cb the code builder to store the local variable in.
     */
    public void store(final CodeBuilder cb) {
        cb.storeLocal(type.kind(), slot);
    }

    /**
     * Returns a {@link Value} which represents this local variable in bytecode.
     * The value load instruction is emitted via the code builder on demand when
     * the bytecode for using it should be generated.
     *
     * @return the created value.
     */
    public Value load() {
        return new Value() {

            @Override
            public JvmType type() {
                return type;
            }

            @Override
            public void emit(final CodeBuilder cb) {
                cb.loadLocal(type.kind(), slot);
            }
        };
    }
}
