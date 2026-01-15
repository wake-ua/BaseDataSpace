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

package org.eclipse.edc.identity.api;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.spi.monitor.Monitor;

/**
 * Endpoint to validate the participant identity
 */
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/")
public class IamIdentityApiController {

    private final Monitor monitor;

    /**
     * Instantiates the controller for the verify identity endpoint
     *
     * @param monitor logger object
     */
    public IamIdentityApiController(Monitor monitor) {
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
        monitor.info("Verify received a health request");
        return "{\"response\":\"IdentityProvider: I'm alive!\"}";
    }
}
