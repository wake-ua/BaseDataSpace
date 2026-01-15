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

import org.eclipse.edc.heleade.commons.verification.claims.checker.FcParticipantClaimChecker;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.Map;
import java.util.Objects;

import static org.eclipse.edc.heleade.policy.extension.evaluation.common.Utils.getParticipantClaim;

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
    protected final String leftOperand;
    /**
     * The checker used to validate participant claims.
     */
    protected final FcParticipantClaimChecker participantClaimChecker;

    /**
     * Creates a new abstract constraint function.
     *
     * @param monitor                 the monitor used for logging
     * @param leftOperand                the key of the claim to evaluate
     * @param participantClaimChecker the checker used to validate participant claims
     */
    protected AbstractConstraintFunction(Monitor monitor, String leftOperand,
                                         FcParticipantClaimChecker participantClaimChecker) {
        this.monitor = monitor;
        this.leftOperand = leftOperand;
        this.participantClaimChecker = participantClaimChecker;
    }


    @Override
    public  boolean evaluate(Operator operator, Object rightValue, R rule, C context) {
        var participantData = context.participantAgent().getClaims();
        var participantId = participantData.get("client_id").toString();
        Map<String, Object> participantClaims = (Map<String, Object>) participantData.get("claims");

        String participantSignedClaims = participantData.containsKey("signedClaims")
                ? participantData.get("signedClaims").toString()
                : null;

        if (participantSignedClaims == null) {
            monitor.severe("Participant signed claims are null");
            return false;
        }

        String participantValueToVerify;

        if (!Objects.equals(leftOperand, "participant_id")) {
            participantValueToVerify = getParticipantClaim(participantClaims, leftOperand);
        } else {
            participantValueToVerify = participantId;
        }

        if (participantValueToVerify == null) {
            monitor.severe("ParticipantValueToVerify  is null or is empty " + leftOperand);
            return false;
        }

        boolean valid = participantClaimChecker.verifyClaims(participantId, participantSignedClaims, participantClaims);

        if (!valid) {
            monitor.severe("Verification with participant registry failed");
            return false;
        }

        // Delegate to subclass for the specific evaluation logic
        return evaluateClaim(operator, participantValueToVerify, rightValue, rule, context);
    }

    /**
     * Subclasses implement this to define how to compare the claim to the rightValue.
     *
     * @param operator   the comparison operator
     * @param participantClaimValue the value of the participant's claim
     * @param rightValue the expected value of the constraint
     * @param rule       the permission rule being evaluated
     * @param context    the current policy context
     * @return true if the claim satisfies the constraint; false otherwise
     */
    protected abstract boolean evaluateClaim(Operator operator, String participantClaimValue, Object rightValue,
                                             R rule, C context);



    /**
     * Evaluates a string-based constraint using the specified operator and claim values.
     * This method determines whether the relationship between the provided claim value
     * and the expected right value satisfies the given operator. If an unsupported operator
     * is encountered, it logs the issue and returns {@code false}.
     *
     * @param operator      the operator to apply for comparison (e.g., EQ, IN); must not be {@code null}.
     * @param participantClaimValue    the value of the participant's claim to evaluate; may be {@code null}.
     * @param rightValue    the expected value to compare against the claim value; must not be {@code null}.
     * @param problemPrefix a prefix used for logging purposes when an issue arises; must not be {@code null}.
     * @return {@code true} if the comparison satisfies the operator, {@code false} otherwise.
     */
    protected boolean evaluateStringOperator(Operator operator, String participantClaimValue, String rightValue, String problemPrefix) {
        return switch (operator) {
            case EQ -> rightValue.equals(participantClaimValue);
            case IN -> rightValue.contains(participantClaimValue);
            default -> {
                monitor.severe((problemPrefix + "Unsupported operator: '%s'").formatted(operator.getOdrlRepresentation()));
                yield false;
            }
        };
    }

}
