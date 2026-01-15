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

package org.eclipse.edc.heleade.commons.verification.claims.checker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.heleade.commons.verification.claims.Claims;
import org.eclipse.edc.spi.monitor.Monitor;

import java.io.IOException;
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
    private final HttpClient httpClient;

    /**
     * Creates a new instance of {@code FcParticipantClaimChecker}.
     *
     * @param monitor the monitor used for logging or tracking claim checks
     * @param baseUrl the base URL of the FC used as a participant registry
     */
    public FcParticipantClaimChecker(Monitor monitor, String baseUrl) {
        this.monitor = monitor;
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Verifies the claims of a participant against the provided signed claims and participant claims data.
     * This method sends an HTTP POST request to the verification endpoint and processes the response
     * to determine the validity of the participant's signature and claims.
     *
     * @param baseUrl the ur of the FC catalog
     * @param participantId the unique identifier of the participant
     * @param signedClaims the signed claims associated with the participant
     * @param participantClaims a map containing the participant's specific claims as key-value pairs
     * @param httpClient an http client to perform the requests
     * @return a {@link VerificationResult} object indicating the success or failure of
     *         signature and claims verification
     * @throws IOException failure during http request
     * @throws InterruptedException failure during http request
     */
    public static VerificationResult verifyClaims(String baseUrl, String participantId, String signedClaims, Map<String, Object> participantClaims, HttpClient httpClient) throws IOException, InterruptedException {
        String url = baseUrl + "verification";

        String json = Claims.getJsonBody(participantId, signedClaims, participantClaims);
        var request = HttpRequest.newBuilder()
                   .uri(URI.create(url))
                   .header("Content-Type", "application/json")
                   .POST(HttpRequest.BodyPublishers.ofString(json))
                   .build();

        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return parseVerificationResponse(response);
    }

    /**
     * Parses the verification response received from an HTTP request and extracts the results of
     * signature and claims verification.
     *
     * @param response the HTTP response containing the JSON body with verification results
     * @return a {@link VerificationResult} object indicating the success or failure of
     *         signature and claims verification
     */
    public static VerificationResult parseVerificationResponse(HttpResponse<String> response) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> jsonMap =
                    mapper.readValue(response.body(), new TypeReference<>() {});

            Map<String, Object> verifySignature =
                    (Map<String, Object>) jsonMap.get("verifySignatureSuccess");

            Map<String, Object> verifyClaims =
                    (Map<String, Object>) jsonMap.get("verifyClaimsSuccess");

            boolean signatureOk =
                    Boolean.parseBoolean((String) verifySignature.get("valueType"));

            boolean claimsOk =
                    Boolean.parseBoolean((String) verifyClaims.get("valueType"));

            return new VerificationResult(signatureOk, claimsOk);
        } catch (Exception e) {
            return new VerificationResult(false, false);
        }

    }

    /**
     * Verifies the claims of a participant against the provided signed claims and participant claims data.
     * This method sends an HTTP POST request to the verification endpoint and processes the response
     * to determine the validity of the participant's signature and claims.
     *
     * @param participantId the unique identifier of the participant
     * @param signedClaims the signed claims associated with the participant
     * @param participantClaims a map containing the participant's specific claims as key-value pairs
     * @return {@code true} if both the signature verification and claims verification are successful,
     *         {@code false} otherwise
     */
    @Override
    public boolean verifyClaims(String participantId, String signedClaims, Map<String, Object> participantClaims) {
        try {

            VerificationResult result = verifyClaims(this.baseUrl, participantId, signedClaims, participantClaims, this.httpClient);

            if (!result.signatureResult()) {
                monitor.warning("Signature verification failed");
            }

            if (!result.claimsResult()) {
                monitor.warning("Claims verification failed");
            }

            return result.signatureResult() && result.claimsResult();

        } catch (Exception e) {
            monitor.warning("Failed to verify claims" + e.getMessage());
            return false;
        }

    }

}
