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
import org.eclipse.edc.heleade.identity.IamIdentityService;
import org.eclipse.edc.spi.monitor.Monitor;

/**
 * Endpoint to validate the participant identity
 */
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/")
public class IamIdentityApiController {

    private final Monitor monitor;
    private final IamIdentityService iamIdentityService;

    /**
     * Instantiates the controller for the verify identity endpoint
     *
     * @param iamIdentityService identity service
     * @param monitor logger object
     */
    public IamIdentityApiController(IamIdentityService iamIdentityService, Monitor monitor) {
        this.iamIdentityService = iamIdentityService;
        this.monitor = monitor;
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

        return "{\"response\":\"IdentityProvider: I'm alive!\"}";
    }
}
