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
import org.eclipse.edc.connector.dataplane.spi.pipeline.InputStreamDataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

/**
 * ServiceDataSource is an implementation of the DataSource interface that processes data
 * using a specified base URL and credential information. It allows creating a stream of data
 * parts that include these configurations serialized in JSON format.
 */
public class ServiceDataSource implements DataSource {

    private final String credentialsServiceUrl;
    private final String credentials;
    private final String baseUrl;

    /**
     * Constructs a new ServiceDataSource using the provided request and credential information.
     * It extracts the base URL from the request's source data address and initializes the data source.
     *
     * @param request               the DataFlowStartMessage containing the source data address with the base URL
     * @param credentialsServiceUrl the URL for the credentials service
     * @param credentials           the credentials required for accessing the service
     */
    public ServiceDataSource(DataFlowStartMessage request, String credentialsServiceUrl, String credentials) {
        this.credentialsServiceUrl = credentialsServiceUrl;
        this.credentials = credentials;
        this.baseUrl = request.getSourceDataAddress().getStringProperty("baseUrl");
    }

    @Override
    public StreamResult<Stream<Part>> openPartStream() {
        String source = "{\"baseUrl\": \"" + baseUrl + "\", " +
                "\"credentialsServiceUrl\": \"" + credentialsServiceUrl + "\", " +
                "\"credentials\": " + credentials + "}";
        InputStream stream = new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8));
        InputStreamDataSource part = new InputStreamDataSource("ServiceDataSource", stream);
        return StreamResult.success(Stream.of(part));
    }

    @Override
    public void close() {
    }

}
