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

import org.eclipse.edc.connector.controlplane.contract.spi.policy.ContractNegotiationPolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.Objects;

import static java.lang.String.format;


public class LocationConstraintFunction extends AbstractConstraintFunction<ContractNegotiationPolicyContext> {



    public LocationConstraintFunction(Monitor monitor, String participantId, String locationKey,  ParticipantClaimChecker participantClaimChecker) {
        super(monitor, participantId, locationKey, participantClaimChecker);
    }

    @Override
    protected boolean evaluateClaim(Operator operator, String claimValue, Object rightValue,
                                    Permission rule, ContractNegotiationPolicyContext context) {
        if (operator != Operator.EQ) {
            monitor.severe("Invalid operator for location constraint: only EQ is allowed.");
            return false;
        }

        boolean match = Objects.equals(claimValue, rightValue.toString());
        monitor.info(format("Evaluating location constraint: %s %s %s -> %s",
                claimKey, operator, rightValue, match));
        return match;
    }

}
