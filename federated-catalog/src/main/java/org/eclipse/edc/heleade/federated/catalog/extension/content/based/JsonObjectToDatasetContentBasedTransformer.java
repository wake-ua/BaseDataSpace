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

package org.eclipse.edc.heleade.federated.catalog.extension.content.based;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.catalog.transform.JsonObjectToDatasetTransformer;
import org.eclipse.edc.connector.controlplane.catalog.spi.DataService;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.connector.controlplane.catalog.spi.Distribution;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.edc.heleade.commons.content.based.catalog.CbmConstants.CBM_HAS_DATA_DICTIONARY;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_ACCESS_SERVICE_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DISTRIBUTION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_ENDPOINT_DESCRIPTION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_ENDPOINT_URL_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCT_FORMAT_ATTRIBUTE;

/**
 * Transformer class for converting a JSON object into a Dataset with additional content-based
 * adjustments specific to federated catalog implementations.
 *
 * Features:
 * - Extends {@code JsonObjectToDatasetTransformer} to inherit base transformation functionality.
 * - Relocates data dictionary information from distribution arrays to the root dataset object when present.
 * - Fixes issues with default transformer including the data access URL in the result
 */
public class JsonObjectToDatasetContentBasedTransformer extends JsonObjectToDatasetTransformer {

    @Override
    public @Nullable Dataset transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var modifiedObject = moveDataDictionaryToDataset(object);
        Dataset dataset = super.transform(modifiedObject, context);
        Dataset fixedDataset = fixDataset(dataset, modifiedObject);
        return fixedDataset;
    }

    private JsonObject moveDataDictionaryToDataset(JsonObject object) {
        // Get a distribution array if it exists
        var distributions = object.getJsonArray(DCAT_DISTRIBUTION_ATTRIBUTE);
        if (distributions == null || distributions.isEmpty()) {
            return object;
        }

        // Find first distribution with data dictionary
        for (var i = 0; i < distributions.size(); i++) {
            var distribution = distributions.getJsonObject(i);
            if (distribution.containsKey(CBM_HAS_DATA_DICTIONARY)) {
                // Create a new object with data dictionary at root
                return Json.createObjectBuilder(object)
                        .add(CBM_HAS_DATA_DICTIONARY,
                                distribution.get(CBM_HAS_DATA_DICTIONARY))
                        .build();
            }
        }

        return object;
    }

    private List<Distribution> getDistributions(JsonObject object) {

        // Get a distribution array if it exists
        var distributions = object.getJsonArray(DCAT_DISTRIBUTION_ATTRIBUTE);
        if (distributions == null || distributions.isEmpty()) {
            return List.of();
        }

        // Fix distributions
        ArrayList<Distribution> distributionsList = new ArrayList<>();

        for (var i = 0; i < distributions.size(); i++) {
            var distributionBuilder = Distribution.Builder.newInstance();
            var distribution = distributions.getJsonObject(i);
            distributionBuilder.format(distribution.getJsonArray(DCT_FORMAT_ATTRIBUTE).getJsonObject(0).getString("@id"));
            if (distribution.containsKey(DCAT_ACCESS_SERVICE_ATTRIBUTE)) {
                var accessServiceValue = distribution.getJsonArray(DCAT_ACCESS_SERVICE_ATTRIBUTE).get(0);
                if (accessServiceValue instanceof JsonObject) {
                    var dataServiceBuilder = DataService.Builder.newInstance();
                    JsonObject accessService = (JsonObject) accessServiceValue;
                    dataServiceBuilder.id(accessService.getString("@id"));
                    var endpointDescription = accessService.get(DCAT_ENDPOINT_DESCRIPTION_ATTRIBUTE);
                    var endpointUrl = accessService.get(DCAT_ENDPOINT_URL_ATTRIBUTE);
                    if (endpointDescription instanceof JsonArray) {
                        String endpointDescriptionString = ((JsonArray) endpointDescription).getJsonObject(0).getString("@value");
                        dataServiceBuilder.endpointDescription(endpointDescriptionString);
                    }
                    if (endpointUrl instanceof JsonArray) {
                        String endpointUrlString = ((JsonArray) endpointUrl).getJsonObject(0).getString("@value");
                        dataServiceBuilder.endpointUrl(endpointUrlString);
                    }
                    distributionBuilder.dataService(dataServiceBuilder.build());
                }
            }
            distributionsList.add(distributionBuilder.build());
        }

        return distributionsList;
    }

    private Dataset fixDataset(Dataset dataset, JsonObject jsonDataset) {
        var fixedDistributions = getDistributions(jsonDataset);
        var fixedDatasetBuilder = Dataset.Builder.newInstance();
        fixedDatasetBuilder.id(dataset.getId());
        fixedDatasetBuilder.properties(dataset.getProperties());
        fixedDatasetBuilder.offers(dataset.getOffers());
        fixedDatasetBuilder.distributions(fixedDistributions);
        return fixedDatasetBuilder.build();
    }
}
