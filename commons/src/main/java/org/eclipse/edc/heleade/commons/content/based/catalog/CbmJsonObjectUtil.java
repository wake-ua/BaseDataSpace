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
import jakarta.json.JsonObjectBuilder;

import static org.eclipse.edc.heleade.commons.content.based.catalog.CbmConstants.CBM_HAS_DATA_DICTIONARY;
import static org.eclipse.edc.heleade.commons.content.based.catalog.CbmConstants.CBM_IS_SAMPLE_OF;
import static org.eclipse.edc.heleade.commons.content.based.catalog.CbmConstants.CBM_SAMPLE_TYPE;
import static org.eclipse.edc.heleade.commons.content.based.catalog.CbmConstants.DISTRIBUTION_TAG;
import static org.eclipse.edc.heleade.commons.content.based.catalog.CbmConstants.TYPE_TAG;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCAT_SCHEMA;

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
     * Processes a JSON array of datasets and moves CBM (Content-Based Metadata) fields
     * such as the data dictionary and byte size from the dataset level to the
     * distribution level for each dataset within the array.
     *
     * @param datasets the JSON array of datasets to process
     * @return a new JSON array with modified datasets where CBM fields are moved
     *         to the distribution level
     */
    public static JsonArray moveCbmFieldsToDistributionDatasetArray(JsonArray datasets) {
        var modifiedDatasetBuilder = Json.createArrayBuilder();
        for (JsonObject dataset : datasets.getValuesAs(JsonObject.class)) {
            modifiedDatasetBuilder.add(moveCbmFieldsToDistributionForDataset(dataset));
        }
        return modifiedDatasetBuilder.build();
    }

    /**
     * Moves Content-Based Metadata (CBM) fields, such as the data dictionary and
     * byte size, from the dataset level to the distribution level for a given dataset.
     *
     * @param dataset the dataset to process and modify
     * @return a new JsonObject with CBM fields moved to the distribution level
     */
    public static JsonObject moveCbmFieldsToDistributionForDataset(JsonObject dataset) {
        // if no CBM relaed fields, return dataset
        if (!dataset.containsKey(CBM_HAS_DATA_DICTIONARY) && !dataset.containsKey(DCAT_SCHEMA + "byteSize")) {
            return dataset;
        }

        // move CBM fields
        JsonObjectBuilder datasetBuilder = Json.createObjectBuilder(dataset);
        JsonArray distributions = getAsJsonArray(dataset, DISTRIBUTION_TAG);
        var modifiedDistributionArrayBuilder = Json.createArrayBuilder();

        for (JsonObject distribution : distributions.getValuesAs(JsonObject.class)) {
            JsonObjectBuilder modifiedDistributionBuilder = Json.createObjectBuilder(distribution);
            // move data dictionary
            if (dataset.containsKey(CBM_HAS_DATA_DICTIONARY)) {
                JsonObject dataDictionary = dataset.getJsonObject(CBM_HAS_DATA_DICTIONARY);
                modifiedDistributionBuilder.add(CBM_HAS_DATA_DICTIONARY, dataDictionary);
                datasetBuilder.remove(CBM_HAS_DATA_DICTIONARY);
            }
            // move dcat byteSize
            if (dataset.containsKey(DCAT_SCHEMA + "byteSize")) {
                modifiedDistributionBuilder.add(DCAT_SCHEMA + "byteSize", dataset.getInt(DCAT_SCHEMA + "byteSize"));
                datasetBuilder.remove(DCAT_SCHEMA + "byteSize");
            }
            modifiedDistributionArrayBuilder.add(modifiedDistributionBuilder.build());
        }
        datasetBuilder.add(DISTRIBUTION_TAG, modifiedDistributionArrayBuilder.build());

        return datasetBuilder.build();

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