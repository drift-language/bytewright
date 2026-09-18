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
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.lang.classfile.CodeBuilder;
import java.lang.classfile.Label;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static lombok.AccessLevel.PRIVATE;


/**
 * Manages scopes, providing a method to declare variables within them and
 * returning a way to access them later on, and a context for the variable's
 * closure.
 *
 * @author Jonathan (GitHub: belicfr)
 */
@RequiredArgsConstructor
public class ScopeManager {

    @Getter
    @NonNull private final CodeBuilder builder;

    /** The next available variable slot index in this scope manager. */
    private int nextSlotIndex = 0;


    /**
     * Instantiates and returns a new scope without a parent.
     *
     * @return a new scope instance without a parent, which means this is the
     *         main scope.
     */
    public Scope scope() {
        return new Scope(this, null);
    }

    /**
     * Instantiates and returns a new scope with the specified parent. The new
     * scope is a sub-scope of the parent scope, meaning it will have access to
     * all the variables declared in its parent scope and all the ones in its
     * ancestors' scopes as well.
     * <p>
     * The provided parent must depend on the current {@link ScopeManager}
     * instance.
     *
     * @param parent the parent scope.
     * @return a new scope instance that is a sub-scope of the specified parent.
     */
    public Scope scope(@NonNull final Scope parent) {
        if (parent.getManager() != this)
            throw new UnexpectedScopeByManagerException();

        return new Scope(this, parent);
    }


    /**
     * A scope of variable declarations. It permits variable declaration and
     * closure.
     */
    @RequiredArgsConstructor(access = PRIVATE)
    public class Scope implements AutoCloseable {

        /**
         * The {@link ScopeManager#nextSlotIndex} is saved in the scope's
         * instance to be restored after closing.
         * <p>
         * This field must be final as it is a snapshot of the current 'next
         * slot index' when the scope instance is created.
         */
        private final int nextSlotIndexSnapshot = nextSlotIndex;

        /**
         * The {@link ScopeManager} that created this {@link Scope}.
         */
        @Getter
        @NonNull private final ScopeManager manager;

        /**
         * The parent scope if any, or {@code null} otherwise.
         */
        @Getter
        private final Scope parent;

        /**
         * The list of local variable declarations in this scope, ordered by
         * their declaration order within this scope.
         */
        private final List<DeclaredLocal> locals = new ArrayList<>();

        /**
         * If the scope is closed or not. A scope is considered as closed after
         * it has been closed with the {@link #close() close} method.
         * <p>
         * Once set to {@code true}, this field should not be reset to {@code
         * false}.
         */
        @Getter
        private boolean isClosed = false;


        /**
         * Searches a local variable by its name in this scope or in its
         * parents, if any.
         *
         * @param name the name of the local variable to search for.
         * @return an option containing the declared local variable, if any.
         */
        public Optional<DeclaredLocal> resolve(@NonNull final String name) {
            Optional<DeclaredLocal> fromCurrentScope = locals.stream()
                    .filter(local -> local.name().equals(name))
                    .findFirst();

            if (fromCurrentScope.isPresent())
                return fromCurrentScope;

            if (hasParent())
                return parent.resolve(name);

            return Optional.empty();
        }

        /**
         * Returns if the provided name is bound within this scope.
         *
         * @param name the name of the variable to check for binding.
         * @return {@code true} if the name is bound, {@code false} otherwise.
         */
        public boolean isNameBoundInCurrentScope(@NonNull final String name) {
            return locals.stream()
                    .anyMatch(local -> local.name().equals(name));
        }

        /**
         * Declares a local variable with given name and type, and returns it.
         * The local's index is assigned, and the structure is saved into the
         * {@link #locals} collection.
         *
         * @param name the name of the local variable.
         * @param type the type of the local variable.
         */
        public Local declare(@NonNull final String name, @NonNull final JvmType type) {
            requireNotClosed();

            if (isNameBoundInCurrentScope(name))
                throw new AlreadyBoundNameException(name);


            final int slot = nextSlotIndex;
            nextSlotIndex += type.slotSize();

            final Label start = builder.newLabel();
            builder.labelBinding(start);

            locals.add(new DeclaredLocal(name, slot, type, start));

            return new Local(slot, type);
        }

        /**
         * Creates and returns a new sub-scope, which will have access to all
         * the variables declared in this scope and all the ones in its
         * ancestors' scopes as well.
         *
         * @return the created sub-scope instance.
         */
        public Scope scope() {
            return manager.scope(this);
        }

        public CodeBuilder getBuilder() {
            return manager.getBuilder();
        }

        /**
         * Returns if this scope has a parent or not.
         *
         * @return {@code true} if the scope has a parent, {@code false}
         *         otherwise.
         */
        public boolean hasParent() {
            return parent != null;
        }

        /**
         * @throws ScopeAlreadyClosedException if the scope is closed.
         */
        public void requireNotClosed() {
            if (isClosed) throw new ScopeAlreadyClosedException();
        }


        @Override
        public void close() {
            requireNotClosed();

            isClosed = true;

            resetSnapshotNextSlotIndex();

            final Label endLabel = builder.newLabel();
            builder.labelBinding(endLabel);

            locals.forEach(local -> {
                builder.localVariable(
                        local.slot(),
                        local.name(),
                        local.type().descriptor(),
                        local.label(),
                        endLabel);
            });
        }


        /**
         * This mechanism permits avoiding wasting between scopes: once the
         * scope is closed, its variables cannot be accessed and their slots can
         * be re-used.
         *
         * <pre>{@code
         * { int a = 1 }    // both should use the same slot as 'a' is no
         * { int b = 2 }    // longer accessible when 'b' is declared.
         * }</pre>
         */
        private void resetSnapshotNextSlotIndex() {
            nextSlotIndex = nextSlotIndexSnapshot;
        }
    }
}
