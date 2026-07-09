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

package org.eclipse.edc.heleade.provider.extension.content.based.api.asset;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import org.eclipse.edc.connector.controlplane.api.management.asset.v4.AssetApiV4;
import org.eclipse.edc.connector.controlplane.api.management.asset.v4.AssetApiV4Controller;
import org.eclipse.edc.connector.controlplane.services.spi.asset.AssetService;
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;
import org.eclipse.edc.web.spi.validation.SchemaType;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.heleade.commons.content.based.catalog.CbmConstants.CBM_SAMPLE_TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DATASET_TYPE;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE_TERM;

/**
 * Controller for managing content-based assets via the Asset Management API.
 * Extends the base AssetApiController and implements the AssetApi interface
 * to provide additional handling for content-based assets.
 * This controller supports the creation and updating of assets with JSON-LD
 * representations, integrating validation and transformation processes specific
 * to content-based assets.
 */
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v4/assets-cbm")
public class ContentBasedAssetApiV4Controller extends AssetApiV4Controller implements AssetApiV4 {
    private final TypeTransformerRegistry transformerRegistry;
    private final Monitor monitor;
    private final JsonObjectValidatorRegistry validator;

    /**
     * Constructs a ContentBasedAssetApiV3Controller instance.
     *
     * @param service the AssetService responsible for asset management operations.
     * @param transformerRegistry the registry of type transformers used to convert asset representations.
     * @param monitor the Monitor to log and track operations.
     * @param validator the JsonObjectValidatorRegistry for validating JSON representations of assets.
     */
    public ContentBasedAssetApiV4Controller(AssetService service, TypeTransformerRegistry transformerRegistry,
                                            Monitor monitor, JsonObjectValidatorRegistry validator, SingleParticipantContextSupplier participantContextSupplier) {
        super(service, transformerRegistry, monitor, validator, participantContextSupplier);
        this.validator = validator;
        this.transformerRegistry = transformerRegistry;
        this.monitor = monitor;
    }

    @POST
    @Override
    public JsonObject createAssetV4(
            @SchemaType({"Dataset", "dcat:Dataset", DCAT_DATASET_TYPE,
                    "Sample", "cbm:Sample", CBM_SAMPLE_TYPE}) JsonObject assetJson) {
        //TODO: Set up dataset validation within the interceptor
        monitor.info("Received CBM asset creation request");
        JsonObject edcAssetJson = transformCbmToAsset(assetJson);
        return super.createAssetV4(edcAssetJson);
    }

    @PUT
    @Override
    public void updateAssetV4(
            @SchemaType({"Dataset", "dcat:Dataset", DCAT_DATASET_TYPE,
                    "Sample", "cbm:Sample", CBM_SAMPLE_TYPE}) JsonObject assetJson) {
        monitor.info("Received CBM asset modification request");
        JsonObject edcAssetJson = transformCbmToAsset(assetJson);
        super.updateAssetV4(edcAssetJson);
    }

    @GET
    @Path("{id}")
    @Override
    public JsonObject getAssetV4(@PathParam("id") String id) {
        return transformAssetToCbm(super.getAssetV4(id));

    }

    @POST
    @Path("/request")
    @Override
    public JsonArray requestAssetsV4(@SchemaType(EDC_QUERY_SPEC_TYPE_TERM) JsonObject querySpecJson) {
        var result = super.requestAssetsV4(querySpecJson);
        return transformAssetListToCbm(result);

    }

    @POST
    @Path("/count")
    public String countAssetsV4(@SchemaType(EDC_QUERY_SPEC_TYPE_TERM) JsonObject querySpecJson) {
        var result = super.requestAssetsV4(querySpecJson).size();
        return "{\"count\": " + result + "}";
    }

    @DELETE
    @Path("{id}")
    @Override
    public void removeAssetV4(@PathParam("id") String id) {
        super.removeAssetV4(id);
    }

    private JsonObject transformCbmToAsset(JsonObject cbmJson) {
        String assetType = DCAT_DATASET_TYPE;
        if (cbmJson.containsKey("@type")) {
            var inputType = cbmJson.get("@type");
            String stringType = "";
            if (inputType instanceof JsonArray) {
                stringType = ((JsonArray) inputType).getString(0);
            } else if (inputType instanceof JsonString) {
                stringType = ((JsonString) inputType).getString();
            }
            if (CBM_SAMPLE_TYPE.equals(stringType)) {
                assetType = CBM_SAMPLE_TYPE;
            }
        }
        validator.validate(assetType, cbmJson).orElseThrow(ValidationFailureException::new);

        JsonObject edcAssetJson = transformerRegistry.transform(new CbmJsonObject(cbmJson), JsonObject.class)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
        return edcAssetJson;
    }

    private JsonArray transformAssetListToCbm(JsonArray jsonArray) {
        JsonArrayBuilder cbmArrayBuilder = Json.createArrayBuilder();
        for (JsonValue jsonValue : jsonArray) {
            JsonObject edcAssetJson = jsonValue.asJsonObject();
            JsonObject cbmAssetJson = transformerRegistry.transform(new AssetJsonObject(edcAssetJson), JsonObject.class)
                    .orElseThrow(f -> new EdcException(f.getFailureDetail()));
            cbmArrayBuilder.add(cbmAssetJson);
        }
        return cbmArrayBuilder.build();
    }

    private JsonObject transformAssetToCbm(JsonObject edcAssetJson) {
        JsonObject cbmAssetJson = transformerRegistry.transform(new AssetJsonObject(edcAssetJson), JsonObject.class)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
        return cbmAssetJson;
    }

}
