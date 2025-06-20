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

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.protocol.dsp.catalog.transform.from.JsonObjectFromDatasetTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.heleade.commons.content.based.catalog.CbmJsonObjectUtil.modifySampleDataset;
import static org.eclipse.edc.heleade.commons.content.based.catalog.CbmJsonObjectUtil.moveDataDictionaryToDistributionForDataset;

/**
 * A transformer class for converting datasets into JSON objects with additional processing
 * specific to a federated catalog content-based implementations.
 * Features:
 * - Utilizes JSON and Jackson libraries to facilitate data transformation.
 * - Ensures compatibility with a content-driven approach for dataset handling.
 */
public class JsonObjectFromDatasetContentBasedTransformer extends JsonObjectFromDatasetTransformer {

    /**
     * Constructs a JsonObjectFromDatasetContentBasedTransformer instance with the specified JSON builder factory
     * and object mapper. This transformer is designed to handle content-based transformations of datasets
     * within a federated catalog.
     *
     * @param jsonFactory the JSON builder factory used for constructing JSON objects
     * @param mapper the Jackson object mapper for JSON serialization and deserialization
     */
    public JsonObjectFromDatasetContentBasedTransformer(jakarta.json.JsonBuilderFactory jsonFactory, com.fasterxml.jackson.databind.ObjectMapper mapper) {
        super(jsonFactory, mapper);
    }

    @Override
    public @Nullable JsonObject transform(@NotNull Dataset dataset, @NotNull TransformerContext context) {
        var object = super.transform(dataset, context);
        if (object == null) {
            return null;
        }
        JsonObject modifiedSampleObject = modifySampleDataset(object);
        return moveDataDictionaryToDistributionForDataset(modifiedSampleObject);
    }
}