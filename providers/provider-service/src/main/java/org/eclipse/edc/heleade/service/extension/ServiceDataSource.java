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
    private final String participantId;
    private final String assetId;
    private final String agreementId;
    private final String processId;

    /**
     * Constructs a new ServiceDataSource with required configurations for processing data.
     *
     * @param request the data flow start message containing source details
     * @param credentialsServiceUrl the URL for the credentials service
     * @param credentials the credentials used to access the service
     * @param originalRequest the original data flow start message for fallback values
     */
    public ServiceDataSource(DataFlowStartMessage request, String credentialsServiceUrl, String credentials, DataFlowStartMessage originalRequest) {
        this.credentialsServiceUrl = credentialsServiceUrl;
        this.credentials = credentials;
        this.baseUrl = request.getSourceDataAddress().getStringProperty("baseUrl");
        this.processId = request.getProcessId();
        this.agreementId = request.getAgreementId() == null ? originalRequest.getAgreementId() : request.getAgreementId();
        this.participantId = request.getParticipantId() == null ? originalRequest.getParticipantId() : request.getParticipantId();
        this.assetId = request.getAssetId() == null ? originalRequest.getAssetId() : request.getAssetId();
    }

    @Override
    public StreamResult<Stream<Part>> openPartStream() {
        String source = "{\"baseUrl\": \"" + baseUrl + "\", " +
                "\"credentialsServiceUrl\": \"" + credentialsServiceUrl + "\", " +
                "\"processId\": \"" + processId + "\", " +
                "\"participantId\": \"" + participantId + "\", " +
                "\"assetId\": \"" + assetId + "\", " +
                "\"agreementId\": \"" + agreementId + "\", " +
                "\"credentials\": " + credentials + "}";
        InputStream stream = new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8));
        InputStreamDataSource part = new InputStreamDataSource("ServiceDataSource", stream);
        return StreamResult.success(Stream.of(part));
    }

    @Override
    public void close() {
    }

}
