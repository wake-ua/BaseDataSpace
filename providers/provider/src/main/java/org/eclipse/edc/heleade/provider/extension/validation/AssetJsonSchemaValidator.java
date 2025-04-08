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

import com.networknt.schema.InputFormat;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.api.management.asset.validation.AssetValidator;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;
import org.eclipse.edc.validator.spi.Violation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
        private final JsonSchema assetSchema;
        private final JsonLd jsonLd;

        AssetJsonSchemaCompliance(JsonSchema assetSchema, JsonLd jsonLd) {
            this.assetSchema = assetSchema;
            this.jsonLd = jsonLd;
        }

        @Override
        public ValidationResult validate(JsonObject asset) {
            // Validate first with the default validator
            ValidationResult defaultValidatorResult = AssetValidator.instance().validate(asset);
            if (defaultValidatorResult.failed()) {
                return defaultValidatorResult;
            }

            // Compact the asset JSON so that it looks similar to user's input with the namespace as context
            var assetCompacted = jsonLd.compact(asset);
            var assetCompactedStr = assetCompacted.getContent().toString();

            // validate the asset against the schema
            Set<ValidationMessage> messages = assetSchema.validate(assetCompactedStr, InputFormat.JSON, executionContext -> {
                executionContext.getExecutionConfig().setFormatAssertionsEnabled(true);
            });

            // collect the validation messages and if necessary transform them into violations to show the user
            List<ValidationMessage> validationMessagesList = messages.stream().collect(Collectors.toList());

            if (validationMessagesList.isEmpty()) {
                return ValidationResult.success();
            } else {
                List<Violation> violations = new ArrayList<>();
                for (ValidationMessage message : validationMessagesList) {
                    Violation violation = new Violation(message.getMessage(), message.getInstanceLocation().toString(), message.getInstanceNode());
                    violations.add(violation);
                }
                return ValidationResult.failure(violations);
            }
        }
    }
}
