/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.heleade.provider.extension.proxy;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import okhttp3.MediaType;
import org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAuthorizationService;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static jakarta.ws.rs.core.MediaType.WILDCARD;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;
import static java.util.Collections.emptyMap;
import static org.eclipse.edc.heleade.provider.extension.service.ServiceDataPlaneExtension.SERVICE_DATA_TYPE;
import static org.eclipse.edc.heleade.provider.extension.service.ServiceDataSource.requestCredentials;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

@Path("{any:.*}")
@Consumes(WILDCARD)
@Produces(WILDCARD)
public class HttpProxyController {

    private final DataPlaneAuthorizationService authorizationService;
    private final EdcHttpClient httpClientEdc;
    private final String defaultCredentials;
    private final Monitor monitor;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public HttpProxyController(DataPlaneAuthorizationService authorizationService,
                               EdcHttpClient httpClientEdc, String defaultCredentials, Monitor monitor) {
        this.authorizationService = authorizationService;
        this.httpClientEdc = httpClientEdc;
        this.defaultCredentials = defaultCredentials;
        this.monitor = monitor;
    }

    @GET
    public Response proxyGet(@Context ContainerRequestContext requestContext) {
        var token = requestContext.getHeaderString(AUTHORIZATION);
        if (token == null) {
            return Response.status(UNAUTHORIZED).build();
        }

        var authorization = authorizationService.authorize(token, emptyMap());
        if (authorization.failed()) {
            return Response.status(FORBIDDEN).build();
        }

        var sourceDataAddress = authorization.getContent();

        String transferType = sourceDataAddress.getStringProperty(EDC_NAMESPACE + "type");

        if (transferType != null && transferType.equals(SERVICE_DATA_TYPE)) {

            var requestParameters = requestContext.getUriInfo().getQueryParameters();
            String processId = requestParameters.getFirst("processId");
            String participantId = requestParameters.getFirst("participantId");
            String assetId = requestParameters.getFirst("assetId");
            String agreementId = requestParameters.getFirst("agreementId");
            String credentialsServiceUrl = sourceDataAddress.getStringProperty(EDC_NAMESPACE + "baseUrl", "");
            String credentialServiceApiKey =  sourceDataAddress.getStringProperty(EDC_NAMESPACE + "authKey", "");
            String credentialServiceApiCode =  sourceDataAddress.getStringProperty(EDC_NAMESPACE + "authCode", "");

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
                credentialsString = requestCredentials(source, credentialsServiceUrl, credentialServiceApiKey, credentialServiceApiCode, httpClientEdc);
            }
            if (credentialsString == null || credentialsString.isEmpty()) {
                monitor.severe("Failed to build ServiceDataSource: No credentials received or no default credentials provided.");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\": \"\"Failed to build ServiceDataSource: No credentials received or no default credentials provided.\"\"}")
                        .build();
            }
            return Response.status(Response.Status.OK)
                    .header(CONTENT_TYPE, MediaType.get("application/json"))
                    .entity(credentialsString)
                    .build();
        } else {
            try {
                var targetUrl = sourceDataAddress.getStringProperty(EDC_NAMESPACE + "baseUrl") + "/" + requestContext.getUriInfo().getPath();
                var request = HttpRequest.newBuilder()
                        .uri(URI.create(targetUrl))
                        .method(requestContext.getMethod(), HttpRequest.BodyPublishers.ofInputStream(requestContext::getEntityStream))
                        .build();
                var response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                return Response.status(response.statusCode())
                        .header(CONTENT_TYPE, response.headers().firstValue(CONTENT_TYPE).orElse(APPLICATION_OCTET_STREAM))
                        .entity(response.body())
                        .build();
            } catch (IOException | InterruptedException e) {
                return Response.status(Response.Status.BAD_GATEWAY)
                        .entity("{\"error\": \"Failed to contact backend service\"}")
                        .build();
            }
        }
    }
}
