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

import org.eclipse.edc.heleade.policy.extension.claims.checker.FcParticipantClaimChecker;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.DynamicAtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.Objects;

import static org.eclipse.edc.heleade.policy.extension.evaluation.common.Utils.getLeftOperand;
import static org.eclipse.edc.heleade.policy.extension.evaluation.common.Utils.getParticipantClaim;
import static org.eclipse.edc.heleade.policy.extension.evaluation.common.Utils.isNumericComparison;
import static org.eclipse.edc.heleade.policy.extension.evaluation.common.Utils.parseNumericValues;

public class DynamicPolicyFunction<R extends Rule, C extends ParticipantAgentPolicyContext> implements DynamicAtomicConstraintRuleFunction<R, C> {

    protected final Monitor monitor;
    protected final FcParticipantClaimChecker participantClaimChecker;

    public DynamicPolicyFunction(Monitor monitor, FcParticipantClaimChecker participantClaimChecker) {
        this.monitor = monitor;
        this.participantClaimChecker = participantClaimChecker;
    }

    @Override
    public boolean evaluate(Object leftValue, Operator operator, Object rightValue, R rule, C context) {
        var participantClaims = context.participantAgent().getClaims();
        var participantId = participantClaims.get("client_id").toString();
        String leftOperatorValue = getLeftOperand(leftValue);
        String participantClaimToVerify = getParticipantClaim(participantClaims, leftOperatorValue);

        if (participantClaimToVerify == null) {
            monitor.severe("Participant claim is null or is empty " + leftOperatorValue);
            return false;
        }

        boolean valid = participantClaimChecker.checkClaim(leftOperatorValue, participantClaimToVerify, participantId);

        if (!valid) {
            monitor.severe("Claim is not valid. It does not match with the one in FC." + leftOperatorValue);
            return false;
        }

        if (isNumericComparison(operator)) {
            Double leftDouble = parseNumericValues(participantClaimToVerify);
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
            case EQ  -> Objects.equals(participantClaimToVerify, rightValue);
            case NEQ -> !Objects.equals(participantClaimToVerify, rightValue);
            case IN  -> participantClaimToVerify.contains(rightValue.toString());
            default -> false;
        };

    }

    @Override
    public boolean canHandle(Object leftValue) {
        return true;
    }
}
