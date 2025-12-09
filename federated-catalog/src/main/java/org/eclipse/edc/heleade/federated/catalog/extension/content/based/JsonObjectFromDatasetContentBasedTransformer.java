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
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.heleade.commons.content.based.catalog.CbmJsonObjectUtil.modifySampleDataset;
import static org.eclipse.edc.heleade.commons.content.based.catalog.CbmJsonObjectUtil.moveCbmFieldsToDistributionForDataset;

/**
 * A transformer class that extends the {@code JsonObjectFromDatasetTransformer} to provide
 * additional content-based transformations for datasets represented as JSON objects.
 *
 * This transformer performs the following transformations:
 * 1. Modifies datasets by tagging sample datasets with a specific type tag using
 *    {@code modifySampleDataset(JsonObject)}.
 * 2. Relocates the data dictionary from the dataset level to the distribution level using
 *    {@code moveDataDictionaryToDistributionForDataset(JsonObject)}.
 *
 * Usage involves transforming a {@code Dataset} into a JSON object with applied content-based
 * modifications tailored for federated catalog implementations.
 */
public class JsonObjectFromDatasetContentBasedTransformer extends JsonObjectFromDatasetTransformer {


    /**
     * Constructs a {@code JsonObjectFromDatasetContentBasedTransformer} to enable transformations
     * of datasets into JSON objects with additional content-based adjustments specific to federated catalog implementations.
     *
     * @param jsonFactory The {@code JsonBuilderFactory} used for constructing JSON objects.
     * @param typeManager The {@code TypeManager} to manage serialization and deserialization types.
     * @param typeContext The context defining the type system within which the transformation occurs.
     */
    public JsonObjectFromDatasetContentBasedTransformer(jakarta.json.JsonBuilderFactory jsonFactory, TypeManager typeManager, String typeContext) {
        super(jsonFactory, typeManager, typeContext);
    }

    @Override
    public @Nullable JsonObject transform(@NotNull Dataset dataset, @NotNull TransformerContext context) {
        var object = super.transform(dataset, context);
        if (object == null) {
            return null;
        }
        JsonObject modifiedSampleObject = modifySampleDataset(object);
        return moveCbmFieldsToDistributionForDataset(modifiedSampleObject);
    }
}