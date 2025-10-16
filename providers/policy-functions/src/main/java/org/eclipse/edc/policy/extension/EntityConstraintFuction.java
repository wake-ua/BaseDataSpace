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

public class EntityConstraintFuction extends AbstractConstraintFunction<CatalogPolicyContext> {


    public EntityConstraintFuction(Monitor monitor, String participantId, String claimKey,
                                   ParticipantClaimChecker participantClaimChecker) {
        super(monitor, participantId, claimKey, participantClaimChecker);
    }

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

