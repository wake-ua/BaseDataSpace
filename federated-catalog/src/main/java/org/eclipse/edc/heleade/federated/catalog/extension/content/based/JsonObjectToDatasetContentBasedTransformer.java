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
import jakarta.json.JsonObject;
import org.eclipse.edc.catalog.transform.JsonObjectToDatasetTransformer;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.heleade.commons.content.based.catalog.CbmConstants;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JsonObjectToDatasetContentBasedTransformer extends JsonObjectToDatasetTransformer {
    @Override
    public @Nullable Dataset transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var modifiedObject = moveDataDictionaryToDataset(object);
        var dataset = super.transform(modifiedObject, context);
        return dataset;
    }

    private JsonObject moveDataDictionaryToDataset(JsonObject object) {
        // Get a distribution array if it exists
        var distributions = object.getJsonArray(CbmConstants.DISTRIBUTION_TAG);
        if (distributions == null || distributions.isEmpty()) {
            return object;
        }

        // Find first distribution with data dictionary
        for (var i = 0; i < distributions.size(); i++) {
            var distribution = distributions.getJsonObject(i);
            if (distribution.containsKey(CbmConstants.CBM_HAS_DATA_DICTIONARY)) {
                // Create a new object with data dictionary at root
                return Json.createObjectBuilder(object)
                        .add(CbmConstants.CBM_HAS_DATA_DICTIONARY,
                                distribution.get(CbmConstants.CBM_HAS_DATA_DICTIONARY))
                        .build();
            }
        }

        return object;
    }
}
