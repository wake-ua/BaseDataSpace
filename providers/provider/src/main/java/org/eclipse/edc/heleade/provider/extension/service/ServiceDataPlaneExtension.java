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

package org.eclipse.edc.heleade.provider.extension.service;

import org.eclipse.edc.connector.dataplane.iam.service.DataPlaneAuthorizationServiceImpl;
import org.eclipse.edc.connector.dataplane.spi.edr.EndpointDataReferenceServiceRegistry;
import org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAccessControlService;
import org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAccessTokenService;
import org.eclipse.edc.connector.dataplane.spi.iam.PublicEndpointGeneratorService;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.validator.spi.DataAddressValidatorRegistry;

import java.time.Clock;


/**
 * ServiceDataPlaneExtension is an implementation of the ServiceExtension interface
 * responsible for initializing and registering a custom DataSourceFactory with the PipelineService.
 * This class enables the handling of "Service"-type data sources in the data flow pipeline.
 */
@Extension(value = ServiceDataPlaneExtension.NAME)
public class ServiceDataPlaneExtension  implements ServiceExtension {

    public static final String NAME = "Provider ServiceDataPlane Extension";

    /**
     * Represents the type identifier for "Service"-type data sources used in the data flow pipeline.
     * This constant is utilized for registering and creating appropriate data source implementations
     * within the PipelineService, specifically ServiceDataSource.
     */
    public static final String SERVICE_DATA_TYPE = "ServiceData";

    @Inject
    PipelineService pipelineService;
    @Inject
    private EndpointDataReferenceServiceRegistry endpointDataReferenceServiceRegistry;
    @Inject
    private EdcHttpClient httpClient;
    @Inject
    private DataAddressValidatorRegistry dataAddressValidatorRegistry;
    @Inject
    private Clock clock;
    @Inject
    private DataPlaneAccessTokenService accessTokenService;
    @Inject
    private DataPlaneAccessControlService accessControlService;
    @Inject
    private PublicEndpointGeneratorService endpointGenerator;

    private DataPlaneAuthorizationServiceImpl dataPlaneAuthorizationService;


    @Override
    public void initialize(ServiceExtensionContext context) {
        String defaultCredentials = context.getConfig().getString("edc.heleade.service.dataservice.credentials.default", "");
        pipelineService.registerFactory(new ServiceDataSourceFactory(context.getMonitor(), httpClient, defaultCredentials));

        var service = getDataPlaneAuthorizationService(context);
        endpointDataReferenceServiceRegistry.register(SERVICE_DATA_TYPE, service);
        endpointDataReferenceServiceRegistry.registerResponseChannel(SERVICE_DATA_TYPE, service);

        var validator = new ServiceDataDataAddressValidator();
        dataAddressValidatorRegistry.registerSourceValidator(SERVICE_DATA_TYPE, validator);
    }

    private DataPlaneAuthorizationServiceImpl getDataPlaneAuthorizationService(ServiceExtensionContext context) {
        if (dataPlaneAuthorizationService == null) {
            dataPlaneAuthorizationService = new DataPlaneAuthorizationServiceImpl(accessTokenService, endpointGenerator, accessControlService, context.getComponentId(), clock);
        }
        return dataPlaneAuthorizationService;
    }
}

