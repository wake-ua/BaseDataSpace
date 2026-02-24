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
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.heleade.commons.content.based.catalog.CbmConstants;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_ASSET_TYPE;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.PROPERTY_ID;
import static org.eclipse.edc.heleade.commons.content.based.catalog.CbmConstants.CBM_SCHEMA;
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

        // Create already the distribution builder
        JsonArrayBuilder distributionArrayBuilder = Json.createArrayBuilder();
        JsonObjectBuilder distributionBuilder = Json.createObjectBuilder();

        // Add context from original object
        if (jsonObject.containsKey("@context")) {
            datasetBuilder.add("@context", jsonObject.get("@context"));
        }

        JsonObject properties = jsonObject.getJsonObject(EDC_NAMESPACE + "properties");

        properties.forEach((key, value) -> {
            if (!isForbiddenProperty(key)) {
                datasetBuilder.add(key, value);
            } else if ((DCAT_SCHEMA + "byteSize").equals(key) || (CBM_SCHEMA + "hasDataDictionary").equals(key)) {
                distributionBuilder.add(key, value);
            }
        });

        // build the data distribution
        JsonObject dataAddress = jsonObject.getJsonObject(EDC_NAMESPACE + "dataAddress");
        JsonObjectBuilder format = Json.createObjectBuilder();
        distributionBuilder.add("@type", DCAT_SCHEMA + "Distribution");

        // get format and type
        String dataAddressType = dataAddress.getString(EDC_NAMESPACE + "type");
        format.add("@id", dataAddressType);
        distributionBuilder.add(DCT_SCHEMA + "format", format.build());

        // get url
        var baseUrl = dataAddress.getString(EDC_NAMESPACE + "baseUrl");
        if (baseUrl != null && !baseUrl.isEmpty()) {
            distributionBuilder.add(DCAT_SCHEMA + "accessURL", baseUrl);
        }

        dataAddress.forEach((key, value) -> {
            if (!isForbiddenDistributionProperty(key)) {
                distributionBuilder.add(key, value);
            }
        });

        distributionArrayBuilder.add(distributionBuilder.build());
        datasetBuilder.add(CbmConstants.DISTRIBUTION_TAG, distributionArrayBuilder.build());

        return datasetBuilder.build();
    }

    private static boolean isForbiddenProperty(String key) {
        // Properties that are forbidden
        return "id".equals(key) || PROPERTY_ID.equals(key) || key.startsWith("@") || (DCAT_SCHEMA + "byteSize").equals(key) || (CBM_SCHEMA + "hasDataDictionary").equals(key);
    }

    private static boolean isForbiddenDistributionProperty(String key) {
        // Properties that are forbidden
        return (EDC_NAMESPACE + "type").equals(key) || (EDC_NAMESPACE + "baseUrl").equals(key) || key.startsWith("@");
    }
}