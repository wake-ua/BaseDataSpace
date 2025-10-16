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

public interface ParticipantClaimChecker {
    /**
     * Checks if a participant's claim is true
     *
     * @param claimKey the claim key
     * @param claimValue the claim value
     * @param participantId the participant ID
     * @return true if the claim is valid
     */
    boolean checkClaim(String claimKey, String claimValue, String participantId);
}
