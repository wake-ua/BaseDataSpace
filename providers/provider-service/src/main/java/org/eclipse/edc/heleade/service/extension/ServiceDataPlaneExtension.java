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

package org.eclipse.edc.heleade.service.extension;

import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

/**
 * ServiceDataPlaneExtension is an implementation of the ServiceExtension interface
 * responsible for initializing and registering a custom DataSourceFactory with the PipelineService.
 * This class enables the handling of "Service"-type data sources in the data flow pipeline.
 */
public class ServiceDataPlaneExtension  implements ServiceExtension {

    /**
     * Represents the type identifier for "Service"-type data sources used in the data flow pipeline.
     * This constant is utilized for registering and creating appropriate data source implementations
     * within the PipelineService, specifically ServiceDataSource.
     */
    public static final String SERVICE_TYPE = "ServiceData";

    @Inject
    PipelineService pipelineService;

    @Override
    public void initialize(ServiceExtensionContext context) {
        String credentialServiceUrl = context.getConfig().getString("edc.heleade.service.dataservice.url");
        String credentials = context.getConfig().getString("edc.heleade.service.dataservice.credentials");

        pipelineService.registerFactory(new ServiceDataSourceFactory(context.getMonitor(), credentialServiceUrl, credentials));
    }
}
