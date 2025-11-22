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

package org.eclipse.edc.policy.extension;

import org.eclipse.edc.connector.controlplane.catalog.spi.policy.CatalogPolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.monitor.Monitor;

import static java.lang.String.format;

/**
 * Defines a policy function that evaluates the entity type policy.
 */
public class EntityConstraintFunction extends AbstractConstraintFunction<CatalogPolicyContext> {

    /**
     * This function is responsible for evaluating a participant’s entity type -related claim.
     *
     * @param monitor The monitor for logging
     * @param claimKey the claim key of the claim to check
     * @param participantClaimChecker the participant claims instance to check claims
     */
    public EntityConstraintFunction(Monitor monitor, String claimKey,
                                    ParticipantClaimChecker participantClaimChecker) {
        super(monitor, claimKey, participantClaimChecker);
    }

    /**
     * Defines a location
     *
     * @param operator   the comparison operator
     * @param claimValue the value of the participant's claim
     * @param rightValue the expected value of the constraint
     * @param rule       the permission rule being evaluated
     * @param context    the current policy context
     * @return true if the claim satisfies the constraint; false otherwise
     */
    @Override
    protected boolean evaluateClaim(Operator operator, String claimValue, Object rightValue,
                                    Permission rule, CatalogPolicyContext context) {
        if (operator == Operator.EQ) {
            boolean match = claimValue.equalsIgnoreCase(rightValue.toString());
            monitor.info(format("Entity type check: %s == %s -> %s", claimValue, rightValue, match));
            return match;
        }

        monitor.severe("Unsupported operator for entity constraint: " + operator);
        return false;
    }


}

