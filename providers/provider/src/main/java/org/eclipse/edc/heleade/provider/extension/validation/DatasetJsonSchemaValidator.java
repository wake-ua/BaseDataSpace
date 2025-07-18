/*
 *  Copyright (c) 2025 University of Alicante
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       University of Alicante - Initial implementation
 *
 */

package org.eclipse.edc.heleade.provider.extension.validation;

import com.networknt.schema.JsonSchema;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;


/**
 * Contains the AssetEntryDto validator definition using a JsonSchema
 */
public class DatasetJsonSchemaValidator {
    /**
     * Returns a JSON Schema validator
     *
     * @param datasetSchema json schema parsed into a JsonSchema object
     * @param jsonLd jsonld for compacting the dataset
     * @return a validator including default and json schema
     */
    public Validator<JsonObject> getValidator(JsonSchema datasetSchema, JsonLd jsonLd) {
        return JsonObjectValidator.newValidator()
                .verify(path -> new DatasetJsonSchemaCompliance(datasetSchema, jsonLd))
                .build();
    }

    private static class DatasetJsonSchemaCompliance implements Validator<JsonObject> {
        private final Validator<JsonObject> jsonSchemaValidator;

        DatasetJsonSchemaCompliance(JsonSchema datasetSchema, JsonLd jsonLd) {
            this.jsonSchemaValidator = new JsonSchemaValidator().getValidator(datasetSchema, jsonLd);

        }

        @Override
        public ValidationResult validate(JsonObject dataset) {
            return jsonSchemaValidator.validate(dataset);
        }
    }
}
