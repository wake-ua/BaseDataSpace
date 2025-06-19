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
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogProtocolService;
import org.eclipse.edc.protocol.dsp.catalog.http.api.controller.BaseDspCatalogApiController;
import org.eclipse.edc.protocol.dsp.http.spi.message.ContinuationTokenManager;
import org.eclipse.edc.protocol.dsp.http.spi.message.DspRequestHandler;
import org.eclipse.edc.spi.monitor.Monitor;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.heleade.commons.content.based.catalog.CbmConstants.CBM_HAS_DATA_DICTIONARY;
import static org.eclipse.edc.heleade.commons.content.based.catalog.CbmConstants.CBM_IS_SAMPLE_OF;
import static org.eclipse.edc.heleade.commons.content.based.catalog.CbmConstants.CBM_SAMPLE;
import static org.eclipse.edc.heleade.commons.content.based.catalog.CbmConstants.DATASETS_TAG;
import static org.eclipse.edc.heleade.commons.content.based.catalog.CbmConstants.DISTRIBUTION_TAG;
import static org.eclipse.edc.heleade.commons.content.based.catalog.CbmConstants.TYPE_TAG;
import static org.eclipse.edc.heleade.provider.extension.content.based.catalog.dispatcher.ContentBasedCatalogApiPaths.CBM_BASE_PATH;
import static org.eclipse.edc.heleade.provider.extension.content.based.catalog.dispatcher.ContentBasedCatalogApiPaths.CBM_CATALOG_REQUEST;
import static org.eclipse.edc.protocol.dsp.http.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_NAMESPACE_V_08;


/**
 * Controller class for handling content-based catalog API requests.
 * Extends the functionality of {@code BaseDspCatalogApiController} to provide
 * content-specific modifications for datasets within the catalog.
 *
 * This class handles API requests adhering to the Content-Based Catalog schema (CBM),
 * modifying dataset entries to include schema-specific metadata.
 *
 * The controller is primarily used to handle requests for content-based datasets 
 * and ensures compliance with CBM-specific schemas.
 */
@Consumes({ APPLICATION_JSON })
@Produces({ APPLICATION_JSON })
@Path(CBM_BASE_PATH)
public class ContentBasedCatalogApiController extends BaseDspCatalogApiController {

    private final Monitor monitor;

    /**
     * Constructs a new ContentBasedCatalogApiController instance.
     *
     * @param service the catalog protocol service used for handling catalog operations.
     * @param dspRequestHandler the request handler for DSP-specific requests.
     * @param continuationTokenManager the manager responsible for handling continuation tokens.
     * @param monitor the monitoring instance used for logging and diagnostics.
     */
    public ContentBasedCatalogApiController(CatalogProtocolService service, DspRequestHandler dspRequestHandler, ContinuationTokenManager continuationTokenManager, Monitor monitor) {
        super(service, dspRequestHandler, continuationTokenManager, DATASPACE_PROTOCOL_HTTP, DSP_NAMESPACE_V_08);
        this.monitor = monitor;
    }

    /**
     * Processes a request to retrieve a content-based catalog and modifies the datasets to conform
     * with the CBM schema before returning the result.
     *
     * @param jsonObject the JSON object containing the request payload.
     * @param token the authorization token required to authenticate the request.
     * @param uriInfo context information about the URI of the request.
     * @param continuationToken an optional continuation token to support paginated responses.
     * @return a Response containing the modified catalog conforming to the CBM schema.
     */
    @POST
    @Path(CBM_CATALOG_REQUEST)
    public Response requestCbmCatalog(JsonObject jsonObject, @HeaderParam(AUTHORIZATION) String token, @Context UriInfo uriInfo,
                                   @QueryParam("continuationToken") String continuationToken) {

        monitor.info("Received a catalog request for Content-Based-Catalog");
        Response response = super.requestCatalog(jsonObject, token, uriInfo, continuationToken);

        // Modify the datasets to match CBM schema
        JsonObject object = ((JsonObject) response.getEntity());
        JsonArray datasets = getAsJsonArray(object, DATASETS_TAG);
        JsonArray modifiedSampleDatasets = modifySampleDatasets(datasets);
        JsonArray modifiedDataDictionaryDatasets = moveDataDictionaryToDistribution(modifiedSampleDatasets);

        var newObject = Json.createObjectBuilder(object).add(DATASETS_TAG, modifiedDataDictionaryDatasets).build();
        return Response.fromResponse(response)
                .entity(newObject)
                .build();
    }

    private JsonArray modifySampleDatasets(JsonArray datasets) {
        var modifiedDatasetBuilder = Json.createArrayBuilder();
        for (JsonObject dataset : datasets.getValuesAs(JsonObject.class)) {
            if (dataset.containsKey(CBM_IS_SAMPLE_OF)) {
                JsonObject modifiedDataset = Json.createObjectBuilder(dataset)
                        .add(TYPE_TAG, CBM_SAMPLE)
                        .build();
                modifiedDatasetBuilder.add(modifiedDataset);
            } else {
                modifiedDatasetBuilder.add(dataset);
            }
        }

        return modifiedDatasetBuilder.build();
    }

    private JsonArray moveDataDictionaryToDistribution(JsonArray datasets) {
        var modifiedDatasetBuilder = Json.createArrayBuilder();
        for (JsonObject dataset : datasets.getValuesAs(JsonObject.class)) {
            if (dataset.containsKey(CBM_HAS_DATA_DICTIONARY)) {
                JsonObject dataDictionary = dataset.getJsonObject(CBM_HAS_DATA_DICTIONARY);
                JsonArray distributions = getAsJsonArray(dataset, DISTRIBUTION_TAG);

                var modifiedDistributionBuilder = Json.createArrayBuilder();
                for (JsonObject distribution : distributions.getValuesAs(JsonObject.class)) {
                    JsonObject modifiedDistribution = Json.createObjectBuilder(distribution)
                            .add(CBM_HAS_DATA_DICTIONARY, dataDictionary)
                            .build();
                    modifiedDistributionBuilder.add(modifiedDistribution);
                }

                JsonObject modifiedDataset = Json.createObjectBuilder(dataset)
                        .remove(CBM_HAS_DATA_DICTIONARY)
                        .add(DISTRIBUTION_TAG, modifiedDistributionBuilder.build())
                        .build();
                modifiedDatasetBuilder.add(modifiedDataset);
            } else {
                modifiedDatasetBuilder.add(dataset);
            }
        }

        return modifiedDatasetBuilder.build();
    }

    private JsonArray getAsJsonArray(JsonObject object, String tag) {
        if (!object.containsKey(tag)) {
            return JsonArray.EMPTY_JSON_ARRAY;
        }
        if (object.get(tag).getValueType().equals(jakarta.json.JsonValue.ValueType.ARRAY)) {
            return object.getJsonArray(tag);
        } else {
            return Json.createArrayBuilder().add(object.get(tag)).build();
        }
    }
}