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

import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;

/**
 * Endpoint class for the health check service
 */
public class HealthEndpointExtension implements ServiceExtension {

    @Inject
    WebService webService;

    /**
     * initializes the extension
     *
     * @param context necessary object to get the configuration and logging
     */
    @Override
    public void initialize(ServiceExtensionContext context) {
        String name = context.getConfig().getString("edc.participant.id", "undefined");
        webService.registerResource(new HealthApiController(context.getMonitor(), name));
    }
}
