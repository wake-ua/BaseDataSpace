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

package org.eclipse.edc.heleade.policy.extension.evaluation.dynamicpolicy;

import org.eclipse.edc.heleade.commons.verification.claims.checker.FcParticipantClaimChecker;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.DynamicAtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.Map;
import java.util.Objects;

import static org.eclipse.edc.heleade.policy.extension.evaluation.common.Utils.getLeftOperand;
import static org.eclipse.edc.heleade.policy.extension.evaluation.common.Utils.getParticipantClaim;
import static org.eclipse.edc.heleade.policy.extension.evaluation.common.Utils.isNumericComparison;
import static org.eclipse.edc.heleade.policy.extension.evaluation.common.Utils.parseNumericValues;




/**
 * The {@code DynamicPolicyFunction} class represents a dynamic policy evaluation mechanism used to validate
 * the compliance of a participant's claims with specific rules and conditions. It implements the
 * {@link DynamicAtomicConstraintRuleFunction} interface, providing the capability to evaluate rules
 * in the context of participant agent policies dynamically.
 *
 * @param <R> the type of the rule being evaluated, extending the {@link Rule} class
 * @param <C> the type of the context used during evaluation, extending the {@link ParticipantAgentPolicyContext} class
 */
public class DynamicPolicyFunction<R extends Rule, C extends ParticipantAgentPolicyContext> implements DynamicAtomicConstraintRuleFunction<R, C> {

    /**
     * Represents the monitoring system used to log and observe application behavior.
     * The {@code monitor} instance is primarily utilized for monitoring, debugging,
     * and tracking system operations in the {@code DynamicPolicyFunction}.
     */
    protected final Monitor monitor;
    /**
     * A checker used to validate participant claims within the {@code DynamicPolicyFunction}.
     * This field references an instance of {@link FcParticipantClaimChecker}, which interacts
     * with the Participant List Registry to verify claims associated with a given participant.
     */
    protected final FcParticipantClaimChecker participantClaimChecker;

    /**
     * Constructs a new instance of {@code DynamicPolicyFunction}.
     *
     * @param monitor the monitor used for logging or observing system behavior
     * @param participantClaimChecker the instance of {@code FcParticipantClaimChecker} used
     *        to verify participant claims against a participant registry
     */
    public DynamicPolicyFunction(Monitor monitor, FcParticipantClaimChecker participantClaimChecker) {
        this.monitor = monitor;
        this.participantClaimChecker = participantClaimChecker;
    }

    @Override
    public boolean evaluate(Object leftValue, Operator operator, Object rightValue, R rule, C context) {
        var participantData = context.participantAgent().getClaims();
        var participantId = participantData.get("client_id").toString();

        String participantSignedClaims = participantData.containsKey("signedClaims")
                ? participantData.get("signedClaims").toString()
                : null;

        if (participantSignedClaims == null) {
            monitor.severe("Participant signed claims are null");
            return false;
        }

        String leftValueString = getLeftOperand(leftValue);

        Map<String, Object> participantClaims = (Map<String, Object>) participantData.get("claims");
        String participantValueToVerify = getParticipantClaim(participantClaims, leftValueString);

        if (participantValueToVerify == null) {
            monitor.severe("Participant claim is null or is empty" + leftValue);
            return false;
        }

        boolean valid = participantClaimChecker.verifyClaims(participantId, participantSignedClaims, participantClaims);

        if (!valid) {
            monitor.severe("Verification with participant registry failed");
            return false;
        }

        if (isNumericComparison(operator)) {
            Double leftDouble = parseNumericValues(participantValueToVerify);
            Double rightDouble = parseNumericValues(rightValue.toString());

            if (leftDouble == null || rightDouble == null) {
                return false;
            }

            return switch (operator) {
                case GT  -> leftDouble > rightDouble;
                case GEQ -> leftDouble >= rightDouble;
                case LT  -> leftDouble < rightDouble;
                case LEQ -> leftDouble <= rightDouble;
                default  -> false;
            };
        }

        return switch (operator) {
            case EQ  -> Objects.equals(participantValueToVerify, rightValue);
            case NEQ -> !Objects.equals(participantValueToVerify, rightValue);
            case IN  -> participantValueToVerify.contains(rightValue.toString());
            default -> false;
        };

    }

    @Override
    public boolean canHandle(Object leftValue) {
        return true;
    }
}
