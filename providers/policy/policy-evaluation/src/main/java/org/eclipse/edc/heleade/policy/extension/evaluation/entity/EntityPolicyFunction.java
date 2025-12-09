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

package org.eclipse.edc.heleade.policy.extension.evaluation.entity;

import org.eclipse.edc.heleade.policy.extension.claims.checker.FcParticipantClaimChecker;
import org.eclipse.edc.heleade.policy.extension.evaluation.common.AbstractConstraintFunction;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.spi.monitor.Monitor;

import static java.lang.String.format;

/**
 * Defines a policy function that evaluates the entityType policy.
 */
public class EntityPolicyFunction<R extends Rule, C extends ParticipantAgentPolicyContext>  extends AbstractConstraintFunction<R, C> {

    private static final String PROBLEM_PREFIX = "Failing evaluation because of invalid entityType constraint. ";

    /**
     * This function is responsible for evaluating a participant’s entity-related claim.
     *
     * @param monitor The monitor used for logging
     * @param claimKey The claim key of the claim to check
     * @param participantClaimChecker The participant claim checker instance
     *
     */
    public EntityPolicyFunction(Monitor monitor, String claimKey, FcParticipantClaimChecker participantClaimChecker) {
        super(monitor, claimKey, participantClaimChecker);
    }



    @Override
    protected boolean evaluateClaim(Operator operator, String claimValue, Object rightValue, R rule, C context) {

        if (!(rightValue instanceof String entityTypeString)) {
            monitor.severe(PROBLEM_PREFIX + "Right operand must be a String");
            return false;
        }

        if (operator != Operator.EQ) {
            monitor.severe("Unsupported operator for entity constraint: " + operator);
            return false;
        }

        boolean match = claimValue.equalsIgnoreCase(entityTypeString);
        monitor.info(format("Evaluating entity constraint: %s %s %s -> %s", claimKey, operator, rightValue, match));
        return match;

    }
}
