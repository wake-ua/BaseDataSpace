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

package org.eclipse.edc.heleade.provider.template.extension.template;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.spi.monitor.Monitor;

/**
 * Template class to uae as base for adding custom controllers and endpoints
 */
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/")
public class TemplateApiController {

    private final Monitor monitor;
    private final String name;

    /**
     * HealthApiController provides a quick check to see if the provider is alive and show its identifying name
     *
     * @param monitor log output
     * @param name provider identifier name to show
     */
    public TemplateApiController(Monitor monitor, String name) {
        this.monitor = monitor;
        this.name = name;
    }

    /**
     * Template method to setup a template endpoint
     *
     * @return JSOM response
     */
    @GET
    @Path("template")
    public String checkTemplate() {
        monitor.info("Template Provider received a request");
        return "{\"response\":\"Provider (%s): It works!\"}".formatted(name);
    }
}
