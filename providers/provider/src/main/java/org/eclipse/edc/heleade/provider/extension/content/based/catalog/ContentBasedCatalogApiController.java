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

package org.eclipse.edc.heleade.provider.extension.content.based.catalog;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogError;
import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogProtocolService;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.protocol.dsp.catalog.http.api.controller.BaseDspCatalogApiController;
import org.eclipse.edc.protocol.dsp.http.spi.message.ContinuationTokenManager;
import org.eclipse.edc.protocol.dsp.http.spi.message.DspRequestHandler;
import org.eclipse.edc.protocol.dsp.http.spi.message.PostDspRequest;
import org.eclipse.edc.spi.monitor.Monitor;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.heleade.provider.extension.content.based.catalog.dispatcher.ContentBasedCatalogApiPaths.CBM_BASE_PATH;
import static org.eclipse.edc.heleade.provider.extension.content.based.catalog.dispatcher.ContentBasedCatalogApiPaths.CBM_CATALOG_REQUEST;
import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.SCHEMA_ORG_NAMESPACE;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCAT_SCHEMA;
import static org.eclipse.edc.protocol.dsp.http.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
import static org.eclipse.edc.protocol.dsp.spi.type.DspCatalogPropertyAndTypeNames.DSPACE_TYPE_CATALOG_REQUEST_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_NAMESPACE_V_08;


@Consumes({ APPLICATION_JSON })
@Produces({ APPLICATION_JSON })
@Path(CBM_BASE_PATH)
public class ContentBasedCatalogApiController extends BaseDspCatalogApiController {

    private final CatalogProtocolService service;
    private final DspRequestHandler dspRequestHandler;
    private final ContinuationTokenManager continuationTokenManager;
    private final String protocol;
    private final JsonLdNamespace namespace;
    private final Monitor monitor;


    public ContentBasedCatalogApiController(CatalogProtocolService service, DspRequestHandler dspRequestHandler, ContinuationTokenManager continuationTokenManager, Monitor monitor) {
        super(service, dspRequestHandler, continuationTokenManager, DATASPACE_PROTOCOL_HTTP, DSP_NAMESPACE_V_08);
        this.service = service;
        this.dspRequestHandler = dspRequestHandler;
        this.continuationTokenManager = continuationTokenManager;
        this.protocol = DATASPACE_PROTOCOL_HTTP;
        this.namespace = DSP_NAMESPACE_V_08;
        this.monitor = monitor;
    }

    @POST
    @Path(CBM_CATALOG_REQUEST)
    public Response requestCbmCatalog(JsonObject jsonObject, @HeaderParam(AUTHORIZATION) String token, @Context UriInfo uriInfo,
                                   @QueryParam("continuationToken") String continuationToken) {
        monitor.info("Received a catalog request for Content-Based-Catalog");
        JsonObject messageJson;
        if (continuationToken == null) {
            messageJson = jsonObject;
        } else {
            messageJson = continuationTokenManager.applyQueryFromToken(jsonObject, continuationToken)
                    .orElseThrow(f -> new BadRequestException(f.getFailureDetail()));
        }

        var request = PostDspRequest.Builder.newInstance(CatalogRequestMessage.class, Catalog.class, CatalogError.class)
                .token(token)
                .expectedMessageType(namespace.toIri(DSPACE_TYPE_CATALOG_REQUEST_MESSAGE_TERM))
                .message(messageJson)
                .serviceCall(service::getCatalog)
                .errorProvider(CatalogError.Builder::newInstance)
                .protocol(protocol)
                .build();

        var responseDecorator = continuationTokenManager.createResponseDecorator(uriInfo.getAbsolutePath().toString());
        var response = dspRequestHandler.createResource(request, responseDecorator);

        JsonObject object = ((JsonObject) response.getEntity());

        JsonArray datasets = JsonArray.EMPTY_JSON_ARRAY;
        String datasetsTag = DCAT_SCHEMA + "dataset";
        String isSampleOfTag = "https://w3id.org/cbm/v0.0.1/ns/isSampleOf";
        String typeTag = "@type";
        String sampleType = "https://w3id.org/cbm/v0.0.1/ns/Sample";
        String algorithmTag = SCHEMA_ORG_NAMESPACE + "algorithm";

        if (object.containsKey(datasetsTag)) {
            if (object.get(datasetsTag).getValueType().equals(jakarta.json.JsonValue.ValueType.ARRAY)) {
                datasets = object.getJsonArray(datasetsTag);
            } else {
                JsonArray tempArray = Json.createArrayBuilder().add(object.get(datasetsTag)).build();
                datasets = tempArray;
            }
        }

        var modifiedDatasetBuilder = Json.createArrayBuilder();
        for (JsonObject dataset : datasets.getValuesAs(JsonObject.class)) {
            if (dataset.containsKey(isSampleOfTag)) {
                JsonObject modifiedDataset = Json.createObjectBuilder(dataset)
                        .add(typeTag, sampleType)
                        .add(algorithmTag, "random")
                        .build();
                modifiedDatasetBuilder.add(modifiedDataset);
            } else {
                modifiedDatasetBuilder.add(dataset);
            }
        }
        JsonArray modifiedDatasets = modifiedDatasetBuilder.build();

        var newObject = Json.createObjectBuilder(object).add(datasetsTag, modifiedDatasets).build();
        return Response.fromResponse(response)
                .entity(newObject)
                .build();
    }
}