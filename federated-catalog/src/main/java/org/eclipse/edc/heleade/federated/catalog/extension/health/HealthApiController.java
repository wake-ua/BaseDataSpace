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

package org.eclipse.edc.heleade.federated.catalog.extension.health;


import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.spi.monitor.Monitor;

@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/")
public class HealthApiController {

    private final Monitor monitor;

    public HealthApiController(Monitor monitor) {
        this.monitor = monitor;
    }

    @GET
    @Path("health-fc")
    public String checkHealth() {
        monitor.info("FederatedCatalog received a health request");
        return "{\"response\":\"FederatedCatalog: I'm alive!\"}";
    }
}
