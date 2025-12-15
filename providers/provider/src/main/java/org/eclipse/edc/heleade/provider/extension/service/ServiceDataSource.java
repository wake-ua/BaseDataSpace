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

import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.InputStreamDataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;


/**
 * ServiceDataSource is an implementation of the DataSource interface that processes data
 * using a specified base URL and credential information. It allows creating a stream of data
 * parts that include these configurations serialized in JSON format.
 */
public class ServiceDataSource implements DataSource {

    private final EdcHttpClient httpClient;
    private final Monitor monitor;
    private final String credentialsServiceUrl;
    private final String credentialServiceApiKey;
    private final String credentialServiceApiCode;
    private final String defaultCredentials;
    private final String participantId;
    private final String assetId;
    private final String agreementId;
    private final String processId;

    /**
     * Constructs a ServiceDataSource for managing service-based data flow operations.
     *
     * @param httpClient the HTTP client for sending service requests and handling responses
     * @param request the data flow start message containing source data and metadata
     * @param monitor the monitoring instance for logging and diagnostics
     * @param defaultCredentials the fallback credentials to use if none are specified
     * @param originalRequest the original data flow start message for retrieving fallback information
     */
    public ServiceDataSource(EdcHttpClient httpClient, DataFlowStartMessage request, Monitor monitor,
                             String defaultCredentials, DataFlowStartMessage originalRequest) {
        this.httpClient = httpClient;
        DataAddress dataAddress = request.getSourceDataAddress();
        this.credentialsServiceUrl = dataAddress.getStringProperty(EDC_NAMESPACE + "baseUrl", "");
        this.credentialServiceApiKey =  dataAddress.getStringProperty(EDC_NAMESPACE + "authKey", "");
        this.credentialServiceApiCode =  dataAddress.getStringProperty(EDC_NAMESPACE + "authCode", "");
        this.defaultCredentials = defaultCredentials;
        this.processId = request.getProcessId();
        this.agreementId = request.getAgreementId() == null ? originalRequest.getAgreementId() : request.getAgreementId();
        this.participantId = request.getParticipantId() == null ? originalRequest.getParticipantId() : request.getParticipantId();
        this.assetId = request.getAssetId() == null ? originalRequest.getAssetId() : request.getAssetId();
        this.monitor = monitor;
    }

    @Override
    public StreamResult<Stream<Part>> openPartStream() {
        String source = "{" +
                "\"processId\": \"" + processId + "\", " +
                "\"participantId\": \"" + participantId + "\", " +
                "\"assetId\": \"" + assetId + "\", " +
                "\"agreementId\": \"" + agreementId +
                "}";
        String credentialsString;
        // if no url, go for default credentials
        if (credentialsServiceUrl == null || credentialsServiceUrl.isEmpty()) {
            credentialsString = defaultCredentials;
        } else {
            credentialsString = requestCredentials(source);
        }
        if (credentialsString == null || credentialsString.isEmpty()) {
            monitor.severe("Failed to build ServiceDataSource: No credentials received or no default credentials provided.");
            return StreamResult.error("Failed to build ServiceDataSource: No credentials received or no default credentials provided.");
        }
        InputStream stream = new ByteArrayInputStream(credentialsString.getBytes(StandardCharsets.UTF_8));
        InputStreamDataSource part = new InputStreamDataSource("ServiceDataSource", stream);
        return StreamResult.success(Stream.of(part));
    }

    private String requestCredentials(String requestBody) {
        var request = new okhttp3.Request.Builder().url(credentialsServiceUrl)
                .header(credentialServiceApiKey, credentialServiceApiCode)
                .post(RequestBody.create(requestBody.getBytes(), MediaType.get("application/json")));
        try (var response = httpClient.execute(request.build())) {
            if (response.isSuccessful()) {
                var body = response.body();
                if (body == null) {
                    throw new EdcException(format("Received empty response body transferring HTTP data for request: %s", response.code()));
                }
                String responseBody = body.string();
                response.close();
                return responseBody;
            } else {
                return response.toString();
            }
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public void close() {
    }

}
