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

package org.eclipse.edc.heleade.policy.extension.claims.checker;


import java.util.Map;

/**
 * Defines the contract for validating participant claims
 * within the policy evaluation process.
 */
public interface ParticipantClaimChecker {


    /**
     * Verifies the claims provided by a participant against a signed set of claims.
     *
     * @param participantId    the unique identifier of the participant whose claims are being verified
     * @param signedClaims     the signed representation of the claims provided by the participant
     * @param participantClaims a map containing the participant's claims to be validated, where keys represent
     *                          claim names and values represent claim values
     * @return true if all participant claims are successfully verified and match the signed claims,
     *         false otherwise
     */
    boolean verifyClaims(String participantId, String signedClaims, Map<String, Object> participantClaims);
}
