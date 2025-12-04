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

import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.InputStreamDataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static java.lang.String.format;


/**
 * ServiceDataSource is an implementation of the DataSource interface that processes data
 * using a specified base URL and credential information. It allows creating a stream of data
 * parts that include these configurations serialized in JSON format.
 */
public class ServiceDataSource implements DataSource {

    private final EdcHttpClient httpClient;
    private final String credentialsServiceUrl;
    private final String defaultCredentials;
    private final String baseUrl;
    private final String participantId;
    private final String assetId;
    private final String agreementId;
    private final String processId;

    /**
     * Constructs a new instance of ServiceDataSource.
     *
     * @param httpClient the HTTP client used for communication
     * @param request the data flow start message containing source data details
     * @param credentialsServiceUrl the URL for the credentials service
     * @param defaultCredentials the default credentials required for accessing the service
     * @param originalRequest the original data flow start message used as a fallback for missing details
     */
    public ServiceDataSource(EdcHttpClient httpClient, DataFlowStartMessage request, String credentialsServiceUrl, String defaultCredentials, DataFlowStartMessage originalRequest) {
        this.httpClient = httpClient;
        this.credentialsServiceUrl = credentialsServiceUrl;
        this.defaultCredentials = defaultCredentials;
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
                "\"credentials\": " + defaultCredentials + "}";

        String credentialsString;
        // if no url, go for default credentials
        if (credentialsServiceUrl == null || credentialsServiceUrl.isEmpty()) {
            credentialsString = defaultCredentials;
        } else {
            credentialsString = requestCredentials(credentialsServiceUrl, source);
        }
        InputStream stream = new ByteArrayInputStream(credentialsString.getBytes(StandardCharsets.UTF_8));
        InputStreamDataSource part = new InputStreamDataSource("ServiceDataSource", stream);
        return StreamResult.success(Stream.of(part));
    }

    private String requestCredentials(String url, String requestBody) {
        var request = new okhttp3.Request.Builder().url(url).post(RequestBody.create(requestBody.getBytes(), MediaType.get("application/json")));
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
