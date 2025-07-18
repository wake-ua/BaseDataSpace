/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.heleade.provider.extension.content.based.api.asset;

import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.eclipse.edc.connector.controlplane.api.management.asset.v3.AssetApi;
import org.eclipse.edc.connector.controlplane.api.management.asset.v3.AssetApiController;
import org.eclipse.edc.connector.controlplane.services.spi.asset.AssetService;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.heleade.commons.content.based.catalog.CbmConstants.CBM_SAMPLE_TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DATASET_TYPE;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v3/assets-cbm")
public class ContentBasedAssetApiController extends AssetApiController implements AssetApi {
    private final TypeTransformerRegistry transformerRegistry;
    private final Monitor monitor;
    private final JsonObjectValidatorRegistry validator;

    public ContentBasedAssetApiController(AssetService service, TypeTransformerRegistry transformerRegistry,
                                          Monitor monitor, JsonObjectValidatorRegistry validator) {
        super(service, transformerRegistry, monitor, validator);
        this.validator = validator;
        this.transformerRegistry = transformerRegistry;
        this.monitor = monitor;
    }

    @POST
    @Override
    public JsonObject createAssetV3(JsonObject assetJson) {
        monitor.info("Received CBM asset creation request");
        if (assetJson.containsKey("@type") && CBM_SAMPLE_TYPE.equals(assetJson.getJsonArray("@type").getString(0))) {
            validator.validate(CBM_SAMPLE_TYPE, assetJson).orElseThrow(ValidationFailureException::new);
        } else {
            validator.validate(DCAT_DATASET_TYPE, assetJson).orElseThrow(ValidationFailureException::new);
        }
        JsonObject edcAssetJson = transformerRegistry.transform(assetJson, JsonObject.class)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
        return super.createAssetV3(edcAssetJson);
    }

    @PUT
    @Override
    public void updateAssetV3(JsonObject assetJson) {
        super.updateAssetV3(assetJson);
    }
}
