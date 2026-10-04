/*******************************************************************************
 * Drift Programming Language
 * ByteWright Library
 *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)
 *
 * This source code is licensed under the Apache 2.0 License.
 * See the LICENSE file in the root directory for details.
 ******************************************************************************/

package dev.driftlang.bytewright.flow;


import dev.driftlang.bytewright.flow.exception.FlowAlreadyStartedException;
import dev.driftlang.bytewright.flow.exception.FlowNotStartedException;
import dev.driftlang.bytewright.flow.exception.UnexpectedSwitchKindException;
import dev.driftlang.bytewright.flow.exception.UnregisteredSwitchCaseValueException;
import dev.driftlang.bytewright.scope.ScopeManager;
import dev.driftlang.bytewright.scope.exception.ScopeAlreadyClosedException;
import lombok.NonNull;

import java.lang.classfile.CodeBuilder;
import java.lang.classfile.Label;
import java.lang.classfile.instruction.SwitchCase;
import java.util.List;
import java.util.Map;

/**
 * Represents a control flow switch. JVM has limitations for switches: it can
 * only handle integers.
 *
 * @author Jonathan (GitHub: belicfr)
 */
public class Switch extends Flow {

    @NonNull private final Map<Integer, Label> caseLabels;
    @NonNull private final Label defaultLabel;


    public Switch(
            @NonNull final ScopeManager.Scope parentScope,
            @NonNull final Map<Integer, Label> caseLabels,
            @NonNull final Label defaultLabel) {

        super(parentScope);
        this.caseLabels = caseLabels;
        this.defaultLabel = defaultLabel;
    }


    /**
     * Searches and returns the {@link Label} linked to the provided value if
     * any; else {@link #defaultLabel}.
     *
     * @param value the value to find the label for.
     * @return the label linked to the value if any; else the default label.
     */
    public Label resolveLabel(final int value) {
        return caseLabels.getOrDefault(value, defaultLabel);
    }

    /**
     * Computes the switch instruction using the heuristic formula {@link
     * Kind#select(int, int, int)}. Once the kind of switch is decided, it calls
     * the appropriate {@link CodeBuilder} switch method between {@link
     * CodeBuilder#tableswitch(Label, List)} and {@link
     * CodeBuilder#lookupswitch(Label, List)}.
     * <p>
     * An unexpected kind of switch — impossible in practice as an enum class is
     * used — would throw a {@link UnexpectedSwitchKindException}.
     *
     * @throws ScopeAlreadyClosedException if this flow's scope is already
     *                                     closed.
     * @throws FlowAlreadyStartedException if this flow has already been
     *                                     started.
     * @throws UnexpectedSwitchKindException if the switch kind is unexpected,
     *                                       see above.
     */
    public void compute() {
        getScope().requireNotClosed();

        start();

        final int lower = getLower();
        final int higher = getHigher();
        final int casesCount = getCasesCount();
        final Kind kind = Kind.select(lower, higher, casesCount);

        final List<SwitchCase> cases = caseLabels.entrySet().stream()
                .map(entry -> SwitchCase.of(entry.getKey(), entry.getValue()))
                .toList();

        switch (kind) {
            case TABLE -> getScope().getBuilder()
                    .tableswitch(defaultLabel, cases);
            case LOOKUP -> getScope().getBuilder()
                    .lookupswitch(defaultLabel, cases);

            default -> throw new UnexpectedSwitchKindException();
        }
    }

    /**
     * Entry point for a switch case body. The label corresponding to the
     * provided value is retrieved if existing and bound. This method does
     * not handle the default case, use {@link #defaultEntry()} instead.
     *
     * @param value the value of the switch case.
     * @throws FlowNotStartedException if this flow has not been started yet.
     * @throws ScopeAlreadyClosedException if this flow's scope is already
     *                                     closed.
     * @throws UnregisteredSwitchCaseValueException if the provided value
     *                                              doesn't correspond to a
     *                                              case.
     */
    public void bodyEntry(final int value) {
        requireStarted();
        getScope().requireNotClosed();

        final Label caseLabel = caseLabels.get(value);

        if (caseLabel != null) {
            getScope().getBuilder()
                    .labelBinding(caseLabel);
        } else {
            throw new UnregisteredSwitchCaseValueException(value);
        }
    }

    /**
     * Entry point for a default switch case body. The default label is bound.
     * As switches must have a default case, this method cannot fail for this
     * reason.
     *
     * @throws FlowNotStartedException if this flow has not been started yet.
     * @throws ScopeAlreadyClosedException if this flow's scope is already
     *                                     closed.
     */
    public void defaultEntry() {
        requireStarted();
        getScope().requireNotClosed();

        getScope().getBuilder()
                .labelBinding(defaultLabel);
    }


    /**
     * @return the lower value of the switch range. If there is no registered
     *         case, it will return 0.
     */
    private int getLower() {
        return caseLabels.keySet().stream()
                .mapToInt(Integer::intValue)
                .min()
                .orElse(0);
    }

    /**
     * @return the higher value of the switch range. If there is no registered
     *         case, it will return 0.
     */
    private int getHigher() {
        return caseLabels.keySet().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
    }

    /**
     * @return the number of cases in that switch.
     */
    private int getCasesCount() {
        return caseLabels.size();
    }


    /**
     * Kind of switch to use.
     * <p>
     * The table switch is interesting for a low range of constants. It creates
     * cases for each integer constant within the specified range.
     * <p>
     * If the range becomes larger, it's better to use the lookup switch that
     * takes pairs of values and targets.
     */
    public enum Kind {

        TABLE,
        LOOKUP
        ;


        /**
         * Heuristic formula to select the best kind of switch, based on the
         * range of constants and the number of cases. The formula is originally
         * defined in {@code com.sun.tools.javac.jvm.Gen#handleSwitch}.
         *
         * @param lower the lower bound of the switch range.
         * @param higher the higher bound of the switch range.
         * @param casesCount the number of cases in the switch.
         * @return the kind of switch to use.
         */
        public static Kind select(final int lower, final int higher, final int casesCount) {
            final long tableSpaceCost = 4 + ((long) higher - lower + 1);
            final long tableTimeCost = 3;
            final long lookupSpaceCost = 3 + 2L * casesCount;
            final long lookupTimeCost = casesCount;

            return casesCount > 0 && tableSpaceCost + 3 * tableTimeCost <= lookupSpaceCost + 3 * lookupTimeCost
                    ? TABLE
                    : LOOKUP;
        }
    }
}
