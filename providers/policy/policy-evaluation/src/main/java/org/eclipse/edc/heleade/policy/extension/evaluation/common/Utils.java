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

import org.eclipse.edc.policy.model.Operator;

import java.util.Map;

public class Utils {

    public static String getParticipantClaim(Map<String, Object> participantClaimsMap, String claimKey) {

        Map<String, Object> participantClaims = (Map<String, Object>) participantClaimsMap.get("claims");

        if (participantClaims == null) {
            return null;
        }

        Object participantSelectedClaim = participantClaims.get(claimKey);

        if (!(participantSelectedClaim instanceof String participantClaimToVerify)) {
            return null;
        }

        if (participantClaimToVerify.isBlank()) {
            return null;
        }

        return participantClaimToVerify;
    }

    public static String getLeftOperand(Object leftOperand) {

        if (leftOperand == null) {
            return null;
        }

        String value = leftOperand.toString();
        int lastSlashIndex = value.lastIndexOf('/');

        if (lastSlashIndex == -1 || lastSlashIndex == value.length() - 1) {
            return value;
        }

        return value.substring(lastSlashIndex + 1);
    }


    public static Double parseNumericValues(
            String value
    ) {
        try {
            return Double.parseDouble(value);

        } catch (NumberFormatException e) {
            return null;
        }
    }


    public static boolean isNumericComparison(Operator operator) {
        return operator == Operator.GT || operator == Operator.GEQ || operator == Operator.LT || operator == Operator.LEQ;
    }

}
