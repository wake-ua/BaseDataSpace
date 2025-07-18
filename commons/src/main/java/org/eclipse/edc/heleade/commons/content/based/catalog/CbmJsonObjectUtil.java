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

package org.eclipse.edc.heleade.commons.content.based.catalog;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;

import static org.eclipse.edc.heleade.commons.content.based.catalog.CbmConstants.CBM_HAS_DATA_DICTIONARY;
import static org.eclipse.edc.heleade.commons.content.based.catalog.CbmConstants.CBM_IS_SAMPLE_OF;
import static org.eclipse.edc.heleade.commons.content.based.catalog.CbmConstants.CBM_SAMPLE_TYPE;
import static org.eclipse.edc.heleade.commons.content.based.catalog.CbmConstants.DISTRIBUTION_TAG;
import static org.eclipse.edc.heleade.commons.content.based.catalog.CbmConstants.TYPE_TAG;

/**
 * Utility class for performing operations on JSON-LD structures related to Content Based Metadata (CBM),
 * including manipulation of datasets and distributions, as well as utility methods for handling JSON arrays.
 */
public class CbmJsonObjectUtil {

    /**
     * Modifies a dataset by adding a sample type tag if it represents a sample dataset.
     *
     * @param dataset the dataset to examine and possibly modify
     * @return a new JsonObject with the sample type tag added if applicable
     */
    public static JsonObject modifySampleDataset(JsonObject dataset) {
        if (dataset.containsKey(CBM_IS_SAMPLE_OF)) {
            return Json.createObjectBuilder(dataset)
                    .add(TYPE_TAG, CBM_SAMPLE_TYPE)
                    .build();
        }
        return dataset;
    }

    /**
     * Modifies a JSON array of datasets by adding sample type tags where applicable.
     *
     * @param datasets the array of datasets to process
     * @return a new JsonArray with modified datasets
     */
    public static JsonArray modifySampleDatasetArray(JsonArray datasets) {
        var modifiedDatasetBuilder = Json.createArrayBuilder();
        for (JsonObject dataset : datasets.getValuesAs(JsonObject.class)) {
            modifiedDatasetBuilder.add(modifySampleDataset(dataset));
        }
        return modifiedDatasetBuilder.build();
    }

    /**
     * Moves the data dictionary from the dataset level to the distribution level in a JSON array
     * of datasets. If a dataset contains a data dictionary, it will be transferred to each of its
     * distributions, and the data dictionary will be removed from the dataset level.
     *
     * @param datasets the JSON array of datasets to process
     * @return a new JSON array with where the data dictionary is moved to the distribution level
     */
    public static JsonArray moveDataDictionaryToDistributionDatasetArray(JsonArray datasets) {
        var modifiedDatasetBuilder = Json.createArrayBuilder();
        for (JsonObject dataset : datasets.getValuesAs(JsonObject.class)) {
            modifiedDatasetBuilder.add(moveDataDictionaryToDistributionForDataset(dataset));
        }
        return modifiedDatasetBuilder.build();
    }

    /**
     * Moves the data dictionary from the dataset level to the distribution level in a JSON object.
     * If the dataset contains a data dictionary, it is added to each distribution within the dataset,
     * and the data dictionary is removed from the dataset level.
     *
     * @param dataset the JSON object representing a dataset to process
     * @return a new JSON object with the data dictionary moved to the distribution level
     */
    public static JsonObject moveDataDictionaryToDistributionForDataset(JsonObject dataset) {
        if (!dataset.containsKey(CBM_HAS_DATA_DICTIONARY)) {
            return dataset;
        }

        JsonObject dataDictionary = dataset.getJsonObject(CBM_HAS_DATA_DICTIONARY);
        JsonArray distributions = getAsJsonArray(dataset, DISTRIBUTION_TAG);

        var modifiedDistributionBuilder = Json.createArrayBuilder();
        for (JsonObject distribution : distributions.getValuesAs(JsonObject.class)) {
            JsonObject modifiedDistribution = Json.createObjectBuilder(distribution)
                    .add(CBM_HAS_DATA_DICTIONARY, dataDictionary)
                    .build();
            modifiedDistributionBuilder.add(modifiedDistribution);
        }

        return Json.createObjectBuilder(dataset)
                .remove(CBM_HAS_DATA_DICTIONARY)
                .add(DISTRIBUTION_TAG, modifiedDistributionBuilder.build())
                .build();
    }

    /**
     * Retrieves a JsonArray from a JsonObject for the specified tag.
     * Returns an empty JsonArray if the tag doesn't exist. If the value
     * at the tag is not already an array, wraps it in a new JsonArray.
     *
     * @param object the JsonObject to extract the array from
     * @param tag the key whose value should be retrieved as a JsonArray
     * @return the JsonArray at the specified tag, or an empty JsonArray if not found
     */
    public static JsonArray getAsJsonArray(JsonObject object, String tag) {
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