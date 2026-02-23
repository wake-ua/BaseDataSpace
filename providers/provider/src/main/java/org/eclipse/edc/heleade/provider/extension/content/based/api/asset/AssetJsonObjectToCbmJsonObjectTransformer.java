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
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.heleade.commons.content.based.catalog.CbmConstants;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_ASSET_TYPE;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.PROPERTY_ID;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCAT_SCHEMA;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCT_SCHEMA;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DATASET_TYPE;
import static org.eclipse.edc.jsonld.spi.TypeUtil.nodeType;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Converts from an {@link Asset} as a {@link JsonObject} in JSON-LD expanded form of a {@link Dataset}.
 */
public class AssetJsonObjectToCbmJsonObjectTransformer extends AbstractJsonLdTransformer<AssetJsonObject, JsonObject> {
    /**
     * Constructor for AssetJsonObjectToCbmJsonObjectTransformer.
     * Configures the transformer to handle transformations from a JsonObject
     * to another JsonObject, leveraging the base functionality provided
     * by the AbstractJsonLdTransformer.
     */
    public AssetJsonObjectToCbmJsonObjectTransformer() {
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

        return transformAssetToCbmJsonObject(id, jsonObject);
    }

    public static JsonObject transformAssetToCbmJsonObject(String id, JsonObject jsonObject) {
        // Check if we have a valid Dataset
        if (!EDC_ASSET_TYPE.equals(nodeType(jsonObject))) {
            return null;
        }

        // Extract the ID
        if (id == null) {
            return null;
        }

        // Start building the Asset object
        var datasetBuilder = Json.createObjectBuilder()
                .add("@id", id)
                .add("@type", DCAT_DATASET_TYPE);

        // Add context from original object
        if (jsonObject.containsKey("@context")) {
            datasetBuilder.add("@context", jsonObject.get("@context"));
        }

        var propertiesValue = jsonObject.get(EDC_NAMESPACE + "properties");
        JsonObject properties = getObjectOrFirstInArray(jsonObject.get(EDC_NAMESPACE + "properties"));

        properties.forEach((key, value) -> {
            if (!isForbiddenProperty(key)) {
                datasetBuilder.add(key, value);
            }
        });

        // build the data distribution
        JsonArrayBuilder distributionArrayBuilder = Json.createArrayBuilder();
        JsonObject dataAddress = getObjectOrFirstInArray(jsonObject.get(EDC_NAMESPACE + "dataAddress"));

        JsonObjectBuilder distributionBuilder = Json.createObjectBuilder();
        JsonObjectBuilder format = Json.createObjectBuilder();

        distributionBuilder.add("@type", DCAT_SCHEMA + "Distribution");

        // get format and type
        String dataAddressType = getStringOrFirstInArray(dataAddress.get(EDC_NAMESPACE + "type"));
        format.add("@id", dataAddressType);
        format.add(DCT_SCHEMA + "type", format.build());
        distributionBuilder.add(DCT_SCHEMA + "format", format.build());

        // get url
        var baseUrlValue = dataAddress.get(EDC_NAMESPACE + "baseUrl");
        if (baseUrlValue != null) {
            String baseUrl = getStringOrFirstInArray(baseUrlValue);
            distributionBuilder.add(DCAT_SCHEMA + "accessURL", baseUrl);
        }

        distributionArrayBuilder.add(distributionBuilder.build());


        datasetBuilder.add(CbmConstants.DISTRIBUTION_TAG, distributionArrayBuilder.build());

        return datasetBuilder.build();
    }

    private static boolean isForbiddenProperty(String key) {
        // Properties that are forbidden
        return "id".equals(key) || PROPERTY_ID.equals(key) || key.startsWith("@");
    }

    private static JsonObject getObjectOrFirstInArray(JsonValue value) {
        if (value.getValueType() == JsonValue.ValueType.ARRAY) {
            return value.asJsonArray().getJsonObject(0);
        } else {
            return value.asJsonObject();
        }
    }

    private static String getStringOrFirstInArray(JsonValue value) {
        if (value.getValueType() == JsonValue.ValueType.ARRAY) {
            JsonObject jsonObject = value.asJsonArray().getJsonObject(0);
            return jsonObject.getString("@value");
        } else if (value.getValueType() == JsonValue.ValueType.OBJECT) {
            return value.asJsonObject().getString("@value");
        } else if (value.getValueType() == JsonValue.ValueType.STRING) {
            return ((JsonString) value).getString();
        }
        return null;
    }

}