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

package org.eclipse.edc.heleade.identity.api;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.heleade.commons.verification.claims.checker.FcParticipantClaimChecker;
import org.eclipse.edc.heleade.commons.verification.claims.checker.VerificationResult;
import org.eclipse.edc.heleade.identity.IamIdentityService;
import org.eclipse.edc.spi.monitor.Monitor;

import java.net.http.HttpClient;

/**
 * Endpoint to validate the participant identity
 */
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/")
public class IamIdentityApiController {

    private final Monitor monitor;
    private final IamIdentityService iamIdentityService;
    private final HttpClient httpClient;
    private final String participantRegistryUrl;

    /**
     * Instantiates the controller for the verify identity endpoint
     *
     * @param iamIdentityService identity service
     * @param participantRegistryUrl federated catalog url
     * @param monitor logger object
     */
    public IamIdentityApiController(IamIdentityService iamIdentityService, String participantRegistryUrl, Monitor monitor) {
        this.iamIdentityService = iamIdentityService;
        this.monitor = monitor;
        this.participantRegistryUrl = participantRegistryUrl;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Defines the verify identity endpoint
     *
     * @return JSON response
     */
    @GET
    @Path("/verify-identity")
    public String verify() {
        monitor.debug("Verify identity received a request");

        try {

            monitor.info("Auto Verification participant node: " + iamIdentityService.getClientId());

            VerificationResult result = FcParticipantClaimChecker.verifyClaims(
                                                    this.participantRegistryUrl,
                                                    iamIdentityService.getClientId(),
                                                    iamIdentityService.getSignedClaims(),
                                                    iamIdentityService.getClaims(),
                                                    httpClient
            );

            return result.asJsonObject().toString();

        } catch (Exception e) {
            monitor.warning("Failed to verify claims" + e.getMessage());
            return "{\"error\": \"Failed to verify claims:" + e.getMessage() + "\"}";
        }

    }
}
