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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.spi.monitor.Monitor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 *  Implementation of {@link ParticipantClaimChecker} that validates participant claims
 *  by retrieving participant information from Participant List Registry.
 */
public class FcParticipantClaimChecker implements ParticipantClaimChecker {
    private final Monitor monitor;
    private final String baseUrl;
    private final String apiKey;

    /**
     * Creates a new instance of {@code FcParticipantClaimChecker}.
     *
     * @param monitor the monitor used for logging or tracking claim checks
     * @param baseUrl the base URL of the FC used as a participant registry
     * @param apiKey  the API key used for authentication in registry
     */
    public FcParticipantClaimChecker(Monitor monitor, String baseUrl, String apiKey) {
        this.monitor = monitor;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    /**
     * Checks if the participant identified by {participantId} has a claim
     * matching the specified key and value. The check is performed by querying the
     * participant registry service and comparing the retrieved data.
     *
     * @param claimKey      the key of the claim to verify
     * @param claimValue    the provided user claim value
     * @param participantId the identifier of the participant
     * @return {@code true} if the participant's claim matches the verified value, {@code false} otherwise or if an error occurs while retrieving the claims
     */
    @Override
    public boolean checkClaim(String claimKey, String claimValue, String participantId) {
        try {
            String url = baseUrl + participantId;
            monitor.info("Checking participant node: " + participantId);
            var client = HttpClient.newHttpClient();
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("x-api-key", apiKey)
                    .GET()
                    .build();

            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> claimsMap = mapper.readValue(response.body(), Map.class);

            Map<String, Object> claims = (Map<String, Object>) claimsMap.get("claims");
            Object value = claims.get(claimKey);
            return value != null && claimValue.equals(value.toString());
        } catch (Exception e) {
            monitor.warning("Failed to fetch participant info: " + e.getMessage());
            return false;
        }
    }

}
