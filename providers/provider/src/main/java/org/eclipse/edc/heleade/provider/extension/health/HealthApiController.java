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

package org.eclipse.edc.heleade.provider.extension.health;


import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.spi.monitor.Monitor;

/**
 * Class that defines the health endpoint for the provider
 */
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/")
public class HealthApiController {

    private final Monitor monitor;
    private final String name;

    /**
     * HealthApiController provides a quick check to see if the provider is alive and show its identifying name
     *
     * @param monitor log output
     * @param name provider identifier name to show
     */
    public HealthApiController(Monitor monitor, String name) {
        this.monitor = monitor;
        this.name = name;
    }

    /**
     * Defines the health endpoint to check connector is running
     *
     * @return JSON string to communicate provider is online
     */
    @GET
    @Path("health")
    public String checkHealth() {
        monitor.info("Provider received a health request");
        return "{\"response\":\"Provider (%s): I'm alive!\"}".formatted(name);
    }
}
