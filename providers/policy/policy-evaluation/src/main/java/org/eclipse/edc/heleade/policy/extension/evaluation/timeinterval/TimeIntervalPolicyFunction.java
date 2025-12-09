/*
 *  Copyright (c) 2025 Universidad de Alicante
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       MO - Universidad de Alicante - initial implementation
 *
 */

package org.eclipse.edc.heleade.policy.extension.evaluation.timeinterval;

import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.spi.monitor.Monitor;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.function.Supplier;


/**
 * Time interval constraint validation function. Checks the time specified in the policy against the current date time.
 */
public class TimeIntervalPolicyFunction<R extends Rule, C extends ParticipantAgentPolicyContext> implements AtomicConstraintRuleFunction<R, C> {

    /**
     * The current date supplier.
     */
    private final Supplier<OffsetDateTime> currentDateSupplier;
    /**
     * The monitor used for logging.
     */
    protected final Monitor monitor;

    /**
     * Creates a new TimeIntervalPolicyFunction that retrieves the current date/time
     * from the provided supplier.
     *
     * @param currentDateSupplier retrieves the current date/time
     *
     * @param monitor the monitor used for logging
    */
    public TimeIntervalPolicyFunction(Supplier<OffsetDateTime> currentDateSupplier, Monitor monitor) {
        this.currentDateSupplier = currentDateSupplier;
        this.monitor = monitor;
    }

    @Override
    public boolean evaluate(Operator operator, Object rightValue, R rule, C context) {
        try {
            var policyDate = OffsetDateTime.parse((String) rightValue);
            var nowDate = currentDateSupplier.get();
            return switch (operator) {
                case LT -> nowDate.isBefore(policyDate);
                case LEQ -> nowDate.isBefore(policyDate) || nowDate.equals(policyDate);
                case GT -> nowDate.isAfter(policyDate);
                case GEQ -> nowDate.isAfter(policyDate) || nowDate.equals(policyDate);
                case EQ -> nowDate.equals(policyDate);
                case NEQ -> !nowDate.equals(policyDate);
                default -> {
                    monitor.warning("Operator '%s' not supported".formatted(operator));
                    yield false;
                }
            };
        } catch (DateTimeParseException e) {
            monitor.severe("Failed to parse right value of constraint to date.");
            return false;
        }
    }

}
