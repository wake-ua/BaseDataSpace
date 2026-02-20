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
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_ASSET_TYPE;
import static org.eclipse.edc.jsonld.spi.TypeUtil.nodeType;

/**
 * Converts from an {@link Asset} as a {@link JsonObject} in JSON-LD expanded form of a {@link Dataset}.
 */
public class JsonObjectAssetToCbmJsonObjectTransformer extends AbstractJsonLdTransformer<AssetJsonObject, JsonObject> {
    /**
     * Constructor for JsonObjectAssetToCbmJsonObjectTransformer.
     * Configures the transformer to handle transformations from a JsonObject
     * to another JsonObject, leveraging the base functionality provided
     * by the AbstractJsonLdTransformer.
     */
    public JsonObjectAssetToCbmJsonObjectTransformer() {
        super(AssetJsonObject.class, JsonObject.class);
    }

    @Override
    public @Nullable JsonObject transform(@NotNull AssetJsonObject assetJsonObject, @NotNull TransformerContext context) {
        JsonObject jsonObject = assetJsonObject.getJsonObject();

        // Check if we have a valid Dataset
        if (!EDC_ASSET_TYPE.equals(nodeType(jsonObject))) {
            context.problem()
                    .unexpectedType()
                    .expected(EDC_ASSET_TYPE)
                    .actual(nodeType(jsonObject))
                    .report();
            return null;
        }

        // Extract the ID
        var id = nodeId(jsonObject);
        if (id == null) {
            context.problem()
                    .missingProperty()
                    .property("@id")
                    .report();
            return null;
        }

        // Start building the Asset object
        var assetBuilder = Json.createObjectBuilder()
                .add("@id", id)
                .add("@type", EDC_ASSET_TYPE);

        // Add context from original object
        if (jsonObject.containsKey("@context")) {
            assetBuilder.add("@context", jsonObject.get("@context"));
        }
        return assetBuilder.build();
    }

}