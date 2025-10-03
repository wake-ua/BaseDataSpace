/*
 *  Copyright (c) 2024 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.policy.extension;

import org.eclipse.edc.connector.controlplane.contract.spi.policy.ContractNegotiationPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.Objects;

import static java.lang.String.format;

/**
 * Evaluates a location-specific constraint within a policy enforcement context.
 * Designed to validate whether a participant's claims meet a defined location-based requirement.
 * Implements an atomic constraint rule function that checks for equality between
 * the region specified in the participant's claims and a provided value.
 * Constructor initializes with a monitor for logging evaluation activity.
 * Constraints evaluated are tied to the "ContractNegotiationPolicyContext."
 * Only supports the equality operator (EQ); reports an issue if an invalid operator is used.
 * Returns true if the participant's region matches the expected value, false otherwise.
 */
public class LocationConstraintFunction implements AtomicConstraintRuleFunction<Permission, ContractNegotiationPolicyContext> {

    private final Monitor monitor;

    /**
     * Constructs a LocationConstraintFunction with a specified monitor.
     *
     * @param monitor an instance of Monitor used for logging evaluation activity
     */
    public LocationConstraintFunction(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public boolean evaluate(Operator operator, Object rightValue, Permission rule, ContractNegotiationPolicyContext context) {

        var region = context.participantAgent().getClaims().get("region");
        monitor.info(context.participantAgent().getClaims().toString());

        if (operator != Operator.EQ) {
            context.reportProblem("Invalid operator, only EQ is allowed!");
            return false;
        }

        monitor.info(format("Evaluating constraint: location %s %s", operator, rightValue.toString()));

        return region != null && Objects.equals(region, rightValue);

    }
}
