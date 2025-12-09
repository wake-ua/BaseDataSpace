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

package org.eclipse.edc.heleade.policy.extension.evaluation.common;

import org.eclipse.edc.heleade.policy.extension.claims.checker.FcParticipantClaimChecker;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.Map;

/**
 * Base class for implementing policy constraint functions that evaluate
 * participant claims within the EDC policy engine.
 * This abstract class provides common logic for retrieving and validating
 * participant claims
 */
public abstract class AbstractConstraintFunction<R extends Rule, C extends  ParticipantAgentPolicyContext> implements AtomicConstraintRuleFunction<R, C> {
    /**
     * The monitor used for logging.
     */
    protected final Monitor monitor;

    /**
     * The key of the claim to evaluate.
     */
    protected final String claimKey;
    /**
     * The checker used to validate participant claims.
     */
    protected final FcParticipantClaimChecker participantClaimChecker;

    /**
     * Creates a new abstract constraint function.
     *
     * @param monitor                 the monitor used for logging
     * @param claimKey                the key of the claim to evaluate
     * @param participantClaimChecker the checker used to validate participant claims
     */
    protected AbstractConstraintFunction(Monitor monitor, String claimKey,
                                         FcParticipantClaimChecker participantClaimChecker) {
        this.monitor = monitor;
        this.claimKey = claimKey;
        this.participantClaimChecker = participantClaimChecker;
    }


    @Override
    public  boolean evaluate(Operator operator, Object rightValue, R rule, C context) {
        var participantClaims = context.participantAgent().getClaims();
        var participantId = participantClaims.get("client_id").toString();

        @SuppressWarnings("unchecked")
        Map<String, Object> participantClaimsMap = (Map<String, Object>) participantClaims.get("claims");
        if (participantClaimsMap == null) {
            monitor.severe("No 'claims' map found in participant claims");
            return false;
        }

        Object participantSelectedClaim = participantClaimsMap.get(claimKey);

        if (participantSelectedClaim == null) {
            monitor.severe("Claim for key: " + claimKey + " is null");
            return false;
        }


        if (!(participantSelectedClaim instanceof String participantClaimToVerify)) {
            monitor.severe("Claim for key: " + claimKey + " is not a string");
            return false;
        }

        if (participantClaimToVerify.isBlank()) {
            monitor.severe("Claim for key: " + claimKey + " is an empty string");
        }


        boolean valid = participantClaimChecker.checkClaim(claimKey, participantClaimToVerify, participantId);
        if (!valid) {
            monitor.severe("Claim is not valid " + claimKey);
            return false;
        }

        // Delegate to subclass for the specific evaluation logic
        return evaluateClaim(operator, participantClaimToVerify, rightValue, rule, context);
    }

    /**
     * Subclasses implement this to define how to compare the claim to the rightValue.
     *
     * @param operator   the comparison operator
     * @param claimValue the value of the participant's claim
     * @param rightValue the expected value of the constraint
     * @param rule       the permission rule being evaluated
     * @param context    the current policy context
     * @return true if the claim satisfies the constraint; false otherwise
     */
    protected abstract boolean evaluateClaim(Operator operator, String claimValue, Object rightValue,
                                             R rule, C context);


}
