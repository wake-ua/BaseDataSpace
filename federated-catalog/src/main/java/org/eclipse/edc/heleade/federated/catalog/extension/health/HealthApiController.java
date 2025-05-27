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
 *       LdE - Universidad de Alicante - initial implementation
 *
 */

package org.eclipse.edc.heleade.federated.catalog.extension.health;


import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.spi.monitor.Monitor;

/**
 * Endpoint to check the federated catalog health
 */
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/")
public class HealthApiController {

    private final Monitor monitor;

    /**
     * Instantiates the controller for the health endpoint
     *
     * @param monitor logger object
     */
    public HealthApiController(Monitor monitor) {
        this.monitor = monitor;
    }

    /**
     * Defines the health endpoint for the federated catalog
     *
     * @return JSON response
     */
    @GET
    @Path("health-fc")
    public String checkHealth() {
        monitor.info("FederatedCatalog received a health request");
        return "{\"response\":\"FederatedCatalog: I'm alive!\"}";
    }
}
