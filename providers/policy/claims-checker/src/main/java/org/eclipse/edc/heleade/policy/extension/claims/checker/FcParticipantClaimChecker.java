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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.spi.monitor.Monitor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Objects;

/**
 *  Implementation of {@link ParticipantClaimChecker} that validates participant claims
 *  by retrieving participant information from Participant List Registry.
 */
public class FcParticipantClaimChecker implements ParticipantClaimChecker {
    private final Monitor monitor;
    private final String baseUrl;

    /**
     * Creates a new instance of {@code FcParticipantClaimChecker}.
     *
     * @param monitor the monitor used for logging or tracking claim checks
     * @param baseUrl the base URL of the FC used as a participant registry
     */
    public FcParticipantClaimChecker(Monitor monitor, String baseUrl) {
        this.monitor = monitor;
        this.baseUrl = baseUrl;
    }

    /**
     * Checks if the participant identified by {participantId} has a claim
     * matching the specified key and value. The check is performed by querying the
     * participant registry service and comparing the retrieved data.
     *
     * @param claimKey      the key of the claim to verify
     * @param participantClaim    the provided user claim to be verified
     * @param participantId the identifier of the participant
     * @return {@code true} if the participant's claim matches the verified value, {@code false} otherwise or if an error occurs while retrieving the claims
     */
    @Override
    public boolean checkClaim(String claimKey, Object participantClaim, String participantId) {
        try {
            String url = baseUrl + participantId;
            monitor.info("Checking participant node: " + participantId);
            var client = HttpClient.newHttpClient();
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                monitor.info("Request failed. Status: " + response.statusCode());
                return false;
            }

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> verifiedClaimsMap = mapper.readValue(response.body(), Map.class);

            Map<String, Object> verifiedClaims = (Map<String, Object>) verifiedClaimsMap.get("claims");
            Object verifiedClaim = verifiedClaims.get(claimKey);
            return Objects.equals(verifiedClaim, participantClaim);
        } catch (Exception e) {
            monitor.warning("Failed to fetch participant info: " + e.getMessage());
            return false;
        }
    }


}
