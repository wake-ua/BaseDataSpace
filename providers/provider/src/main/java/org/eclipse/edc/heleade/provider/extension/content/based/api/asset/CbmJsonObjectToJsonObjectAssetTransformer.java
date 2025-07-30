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

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_ASSET_DATA_ADDRESS;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_ASSET_TYPE;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.PROPERTY_ID;
import static org.eclipse.edc.heleade.commons.content.based.catalog.CbmConstants.CBM_SAMPLE_TYPE;
import static org.eclipse.edc.heleade.commons.content.based.catalog.CbmConstants.CBM_SCHEMA;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCAT_SCHEMA;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCT_SCHEMA;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DATASET_TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DISTRIBUTION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.TypeUtil.nodeType;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Converts from an {@link Dataset} as a {@link JsonObject} in JSON-LD expanded form of an {@link Asset}.
 */
public class CbmJsonObjectToJsonObjectAssetTransformer extends AbstractJsonLdTransformer<JsonObject, JsonObject> {
    /**
     * Constructor for CbmJsonObjectToJsonObjectAssetTransformer.
     * Configures the transformer to handle transformations from a JsonObject
     * to another JsonObject, leveraging the base functionality provided
     * by the AbstractJsonLdTransformer.
     */
    public CbmJsonObjectToJsonObjectAssetTransformer() {
        super(JsonObject.class, JsonObject.class);
    }

    @Override
    public @Nullable JsonObject transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        // Check if we have a valid Dataset
        if (!DCAT_DATASET_TYPE.equals(nodeType(jsonObject)) && !CBM_SAMPLE_TYPE.equals(nodeType(jsonObject))) {
            context.problem()
                    .unexpectedType()
                    .expected(DCAT_DATASET_TYPE)
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

        // Build properties object
        var propertiesArrayBuilder = Json.createArrayBuilder();

        // Process properties - extract properties that should go into the properties object
        var propertiesObjectBuilder = Json.createObjectBuilder();

        AtomicBoolean propertiesValid = new AtomicBoolean(true);
        jsonObject.forEach((key, value) -> {
            if (!isIgnoredProperty(key)) {
                if (isForbiddenProperty(key)) {
                    context.problem()
                            .invalidProperty()
                            .property(key)
                            .report();
                    propertiesValid.set(false);
                }
                // Add property object to array
                propertiesObjectBuilder.add(key, value);
            }
        });

        if (!propertiesValid.get()) {
            return null;
        }

        // Add data address from original object
        if (jsonObject.containsKey(DCAT_DISTRIBUTION_ATTRIBUTE)) {
            JsonObject dcatDistribution = jsonObject.getJsonArray(DCAT_DISTRIBUTION_ATTRIBUTE).getJsonObject(0);
            assetBuilder.add(EDC_ASSET_DATA_ADDRESS, getDataAddress(dcatDistribution));

            //Move data dictionary to object properties
            if (dcatDistribution.containsKey(CBM_SCHEMA + "hasDataDictionary")) {
                propertiesObjectBuilder.add(CBM_SCHEMA + "hasDataDictionary", dcatDistribution.get(CBM_SCHEMA + "hasDataDictionary"));
            }
        }

        // Build and add the properties
        propertiesArrayBuilder.add(propertiesObjectBuilder.build());

        // Add properties array to properties object
        assetBuilder.add(EDC_NAMESPACE + "properties", propertiesArrayBuilder);

        // Add context from original object
        if (jsonObject.containsKey("@context")) {
            assetBuilder.add("@context", jsonObject.get("@context"));
        }

        return assetBuilder.build();
    }

    /**
     * Determines if a property should be ignored when building the properties object.
     */
    private boolean isIgnoredProperty(String key) {
        // Properties that should not go into the properties object
        return "@id".equals(key) || "@type".equals(key) || "@context".equals(key) || DCAT_DISTRIBUTION_ATTRIBUTE.equals(key) || ODRL_POLICY_ATTRIBUTE.equals(key);
    }

    /**
     * Determines if a property is forbidden and method should fail.
     */
    private boolean isForbiddenProperty(String key) {
        // Properties that are forbidden
        return "id".equals(key) || PROPERTY_ID.equals(key);
    }

    private JsonArrayBuilder getDataAddress(JsonObject dataDistribution) {
        var dataAddressArrayBuilder = Json.createArrayBuilder();
        var dataAddressBuilder = Json.createObjectBuilder();
        dataAddressBuilder.add("@type", EDC_ASSET_DATA_ADDRESS);
        dataAddressBuilder.add(EDC_NAMESPACE + "proxyPath", getStringPropertyAsArray(dataDistribution.getJsonArray(EDC_NAMESPACE + "proxyPath").getJsonObject(0).getString("@value")));
        dataAddressBuilder.add(EDC_NAMESPACE + "type", getStringPropertyAsArray(getDataAddressType(dataDistribution)));
        if (dataDistribution.containsKey(DCT_SCHEMA + "title")) {
            dataAddressBuilder.add(EDC_NAMESPACE + "name", getStringPropertyAsArray(dataDistribution.getJsonArray(DCT_SCHEMA + "title").getJsonObject(0).getString("@value")));
        }
        dataAddressBuilder.add(EDC_NAMESPACE + "baseUrl", getStringPropertyAsArray(dataDistribution.getJsonArray(DCAT_SCHEMA + "accessURL").getJsonObject(0).getString("@value")));
        if (dataDistribution.containsKey(EDC_NAMESPACE + "authKey")) {
            dataAddressBuilder.add(EDC_NAMESPACE + "authKey", getStringPropertyAsArray(dataDistribution.getJsonArray(EDC_NAMESPACE + "authKey").getJsonObject(0).getString("@value")));
        }
        if (dataDistribution.containsKey(EDC_NAMESPACE + "authCode")) {
            dataAddressBuilder.add(EDC_NAMESPACE + "authCode", getStringPropertyAsArray(dataDistribution.getJsonArray(EDC_NAMESPACE + "authCode").getJsonObject(0).getString("@value")));
        }
        dataAddressArrayBuilder.add(dataAddressBuilder.build());
        return dataAddressArrayBuilder;
    }

    private JsonArray getStringPropertyAsArray(String value) {
        var propertyArray = Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("@value", value)
                                .build());
        return propertyArray.build();
    }

    private String getDataAddressType(JsonObject dataDistribution) {
        if (dataDistribution.containsKey(DCT_SCHEMA + "format")) {
            String distributionType = dataDistribution.getJsonArray(DCT_SCHEMA + "format").getJsonObject(0).getString("@id");
            if (distributionType.equals("HttpData-PULL") || distributionType.equals("HttpData-PUSH")) {
                return "HttpData";
            }
            return distributionType;
        } else {
            return null;
        }
    }

}