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

import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

import static org.eclipse.edc.heleade.provider.extension.service.ServiceDataPlaneExtension.SERVICE_TYPE;

/**
 * Factory for creating instances of ServiceDataSource for use within a data flow pipeline.
 * Provides support for "Service"-type data sources by configuring them with relevant credential information.
 */
public class ServiceDataSourceFactory implements DataSourceFactory {

    private final Monitor monitor;
    private final EdcHttpClient httpClient;
    private final String credentialServiceUrl;
    private final String credentialServiceApiKeyHeader;
    private final String credentialServiceApiKeyValue;
    private final String defaultCredentials;

    private HashMap<String, DataFlowStartMessage> requestCache = new HashMap<>();

    /**
     * Constructs a ServiceDataSourceFactory for creating instances of ServiceDataSource with specified configurations.
     *
     * @param monitor the monitoring instance for logging and diagnostics
     * @param httpClient the HTTP client for service communication
     * @param credentialServiceUrl the URL of the credential service
     * @param credentialServiceApiKeyHeader the header name for the API key used for authentication
     * @param credentialServiceApiKeyValue the value of the API key used for authentication
     * @param defaultCredentials the default credentials to be used if no service URL is provided
     */
    public ServiceDataSourceFactory(Monitor monitor, EdcHttpClient httpClient,
                                    String credentialServiceUrl, String credentialServiceApiKeyHeader, String credentialServiceApiKeyValue,
                                    String defaultCredentials) {
        this.monitor = monitor;
        this.credentialServiceUrl = credentialServiceUrl;
        this.credentialServiceApiKeyHeader = credentialServiceApiKeyHeader;
        this.credentialServiceApiKeyValue = credentialServiceApiKeyValue;
        this.defaultCredentials = defaultCredentials;
        this.httpClient = httpClient;
    }

    @Override
    public String supportedType() {
        return SERVICE_TYPE;
    }

    @Override
    public DataSource createSource(DataFlowStartMessage request) {
        monitor.info("creating ServiceDataSource with url: " + credentialServiceUrl +
                " for request processId: " + request.getProcessId() + ", " + defaultCredentials);
        DataFlowStartMessage originalRequest = requestCache.get(request.getProcessId());
        requestCache.remove(request.getProcessId());
        return new ServiceDataSource(httpClient, request,
                credentialServiceUrl, credentialServiceApiKeyHeader, credentialServiceApiKeyValue,
                defaultCredentials, originalRequest);
    }

    @Override
    public @NotNull Result<Void> validateRequest(DataFlowStartMessage request) {
        if (request.getParticipantId() == null) {
            return Result.failure("Failed to build ServiceDataSource: Missing participantId");
        }
        requestCache.put(request.getProcessId(), request);
        return Result.success();
    }
}
