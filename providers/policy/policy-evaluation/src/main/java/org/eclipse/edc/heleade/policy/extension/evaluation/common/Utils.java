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

/**
 * Utility methods for performing common operations within the framework.
 */
public class Utils {


    /**
     * Retrieves a participant claim from the provided map based on the given claim key.
     * The method checks if the claims map is valid and contains the specified claim key.
     * If the claim is not a non-blank string, the method returns {@code null}.
     *
     * @param participantClaims the map containing participant claims, where the claims are expected
     *                             to be nested within a key named "claims"; must not be {@code null}.
     * @param claimKey             the key corresponding to the claim to retrieve; must not be {@code null}.
     * @return the claim value as a non-blank string if it exists and is valid;
     *         {@code null} otherwise.
     */
    public static String getParticipantClaim(Map<String, Object> participantClaims, String claimKey) {


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

    /**
     * Extracts and returns the portion of the input object's string representation
     * following the last forward slash ('/'), or the entire string if no forward slash is present.
     * If the input is {@code null}, the method returns {@code null}.
     *
     * @param leftOperand the input object whose string representation is to be processed;
     *                    may be {@code null}.
     * @return the substring following the last forward slash ('/') in the string representation
     *         of the input object, or the full string if no forward slash is present,
     *         or {@code null} if the input is {@code null}.
     */
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


    /**
     * Parses a numeric value from a string and converts it to a {@code Double}.
     * If the provided string is not a valid numeric value, the method returns {@code null}.
     *
     * @param value the string to be parsed into a numeric {@code Double} value;
     *              may be {@code null} or contain non-numeric characters.
     * @return the parsed {@code Double} value if the input is a valid numeric string,
     *         or {@code null} if the input is not numeric or is {@code null}.
     */
    public static Double parseNumericValues(
            String value
    ) {
        try {
            return Double.parseDouble(value);

        } catch (NumberFormatException e) {
            return null;
        }
    }


    /**
     * Determines whether the specified operator is a numeric comparison operator.
     * Numeric comparison operators include greater than (GT), greater than or equal to (GEQ),
     * less than (LT), and less than or equal to (LEQ).
     *
     * @param operator the operator to check; must be non-null and represent one of the
     *                 predefined operators in the {@code Operator} enumeration.
     * @return {@code true} if the operator is one of the numeric comparison operators
     *         (GT, GEQ, LT, LEQ); {@code false} otherwise.
     */
    public static boolean isNumericComparison(Operator operator) {
        return operator == Operator.GT || operator == Operator.GEQ || operator == Operator.LT || operator == Operator.LEQ;
    }

}
