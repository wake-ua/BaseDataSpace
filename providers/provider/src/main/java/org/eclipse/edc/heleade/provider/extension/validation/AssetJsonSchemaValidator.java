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
import org.eclipse.edc.connector.controlplane.api.management.asset.validation.AssetValidator;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;


/**
 * Contains the AssetEntryDto validator definition using a JsonSchema
 */
public class AssetJsonSchemaValidator {
    /**
     * Returns a JSON Schema validator
     *
     * @param assetSchema json schema parsed into a JsonSchema object
     * @param jsonLd jsonld for compacting the asset
     * @return a validator including default and json schema
     */
    public Validator<JsonObject> getValidator(JsonSchema assetSchema, JsonLd jsonLd) {
        return JsonObjectValidator.newValidator()
                .verify(path -> new AssetJsonSchemaCompliance(assetSchema, jsonLd))
                .build();
    }

    private static class AssetJsonSchemaCompliance implements Validator<JsonObject> {
        private final Validator<JsonObject> jsonSchemaValidator;

        AssetJsonSchemaCompliance(JsonSchema assetSchema, JsonLd jsonLd) {
            this.jsonSchemaValidator = new JsonSchemaValidator().getValidator(assetSchema, jsonLd);

        }

        @Override
        public ValidationResult validate(JsonObject asset) {
            // Validate first with the default validator
            ValidationResult defaultValidatorResult = AssetValidator.instance().validate(asset);
            if (defaultValidatorResult.failed()) {
                return defaultValidatorResult;
            }
            return jsonSchemaValidator.validate(asset);
        }
    }
}
