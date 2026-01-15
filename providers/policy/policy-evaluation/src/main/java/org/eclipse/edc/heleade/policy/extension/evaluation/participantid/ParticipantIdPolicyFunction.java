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

package org.eclipse.edc.heleade.policy.extension.evaluation.participantid;

import org.eclipse.edc.heleade.commons.verification.claims.checker.FcParticipantClaimChecker;
import org.eclipse.edc.heleade.policy.extension.evaluation.common.AbstractConstraintFunction;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.spi.monitor.Monitor;

/**
 *
 * Defines a policy function that evaluates the participant id
 */
public class ParticipantIdPolicyFunction<R extends Rule, C extends ParticipantAgentPolicyContext> extends AbstractConstraintFunction<R, C> {

    private static final String PROBLEM_PREFIX = "Failing evaluation because of invalid participant id constraint. ";
    /**
     * This function is responsible for evaluating a participant’s legal-name-related claim.
     *
     * @param monitor The monitor used for logging
     * @param leftOperand The claim key of the claim to check
     * @param participantClaimChecker The participant claim checker instance
     *
     */

    public ParticipantIdPolicyFunction(Monitor monitor, String leftOperand, FcParticipantClaimChecker participantClaimChecker) {
        super(monitor, leftOperand, participantClaimChecker);
    }

    @Override
    protected boolean evaluateClaim(Operator operator, String participantClaimValue, Object rightValue, R rule, C context) {
        if (!(rightValue instanceof String rightValueString)) {
            monitor.severe(PROBLEM_PREFIX + "Right operand must be a String");
            return false;
        }

        return evaluateStringOperator(operator, participantClaimValue, rightValueString, PROBLEM_PREFIX);
    }
}