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

import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.jetbrains.annotations.NotNull;

import static org.eclipse.edc.heleade.service.extension.ServiceDataPlaneExtension.SERVICE_TYPE;

/**
 * Factory for creating instances of ServiceDataSource for use within a data flow pipeline.
 * Provides support for "Service"-type data sources by configuring them with relevant credential information.
 */
public class ServiceDataSourceFactory implements DataSourceFactory {

    private final Monitor monitor;
    private final String credentialServiceUrl;
    private final String credentials;

    /**
     * Constructs a new instance of the ServiceDataSourceFactory to create ServiceDataSource objects.
     *
     * @param monitor the monitor used for logging and diagnostics
     * @param credentialServiceUrl the URL for the credentials service
     * @param credentials the credentials required for accessing the service
     */
    public ServiceDataSourceFactory(Monitor monitor, String credentialServiceUrl, String credentials) {
        this.monitor = monitor;
        this.credentialServiceUrl = credentialServiceUrl;
        this.credentials = credentials;
    }

    @Override
    public String supportedType() {
        return SERVICE_TYPE;
    }

    @Override
    public DataSource createSource(DataFlowStartMessage request) {
        monitor.info("creating ServiceDataSource with url: " + credentialServiceUrl + ", " + credentials);
        return new ServiceDataSource(request, credentialServiceUrl, credentials);
    }

    @Override
    public @NotNull Result<Void> validateRequest(DataFlowStartMessage request) {
        try {
            createSource(request);
        } catch (Exception e) {
            return Result.failure("Failed to build ServiceDataSource: " + e.getMessage());
        }
        return Result.success();
    }
}
