/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.edc.heleade.consumer.extension.health;


import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.spi.monitor.Monitor;

/**
 * Controller class for the health check
 */
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/")
public class HealthApiController {

    private final Monitor monitor;
    private final String name;

    /**
     * Initializes the health controller
     *
     * @param monitor object for logging purposes
     * @param name identifier of teh consumer within the dataspace
     */
    public HealthApiController(Monitor monitor, String name) {
        this.monitor = monitor;
        this.name = name;
    }

    /**
     * outputs a simple line to assess connector health
     *
     * @return string confirming status and name
     */
    @GET
    @Path("health")
    public String checkHealth() {
        monitor.info("Consumer received a health request");
        return "{\"response\":\"Consumer (%s): I'm alive!\"}".formatted(name);
    }
}
