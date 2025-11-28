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

package org.eclipse.edc.heleade.policy.extension.evaluation.location;

import org.eclipse.edc.heleade.policy.extension.claims.checker.FcParticipantClaimChecker;
import org.eclipse.edc.heleade.policy.extension.evaluation.common.AbstractConstraintFunction;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.Objects;

import static java.lang.String.format;

/**
 *
 * Defines a policy function that evaluates the location policy.
 */
public class LocationPolicyFunction<R extends Rule, C extends ParticipantAgentPolicyContext>  extends AbstractConstraintFunction<R, C> {


    /**
     * This function is responsible for evaluating a participant’s location-related claim.
     *
     * @param monitor The monitor used for logging
     * @param claimKey The claim key of the claim to check
     * @param participantClaimChecker The participant claim checker instance
     *
     */
    public LocationPolicyFunction(Monitor monitor, String claimKey, FcParticipantClaimChecker participantClaimChecker) {
        super(monitor, claimKey, participantClaimChecker);
    }


    @Override
    protected boolean evaluateClaim(Operator operator, String claimValue, Object rightValue, R rule, C context) {
        if (operator != Operator.EQ) {
            monitor.severe("Invalid operator for location constraint: only EQ is allowed.");
            return false;
        }

        boolean match = Objects.equals(claimValue, rightValue.toString());
        monitor.info(format("Evaluating location constraint: %s %s %s -> %s", claimKey, operator, rightValue, match));
        return match;
    }
}
