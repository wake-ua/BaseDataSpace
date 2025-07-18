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
 * Contains the object validator definition using a JsonSchema
 */
public class JsonSchemaValidator {
    /**
     * Returns a JSON Schema validator
     *
     * @param jsonSchema json schema parsed into a JsonSchema object
     * @param jsonLd jsonld for compacting the asset
     * @return a validator including default and json schema
     */
    public Validator<JsonObject> getValidator(JsonSchema jsonSchema, JsonLd jsonLd) {
        return JsonObjectValidator.newValidator()
                .verify(path -> new JsonSchemaCompliance(jsonSchema, jsonLd))
                .build();
    }

    private static class JsonSchemaCompliance implements Validator<JsonObject> {
        private final JsonSchema jsonSchema;
        private final JsonLd jsonLd;

        JsonSchemaCompliance(JsonSchema jsonSchema, JsonLd jsonLd) {
            this.jsonSchema = jsonSchema;
            this.jsonLd = jsonLd;
        }

        @Override
        public ValidationResult validate(JsonObject jsonObject) {

            // Compact the jsonObject JSON so that it looks similar to user's input with the namespace as context
            var assetCompacted = jsonLd.compact(jsonObject);
            var assetCompactedStr = assetCompacted.getContent().toString();

            // validate the jsonObject against the schema
            Set<ValidationMessage> messages = jsonSchema.validate(assetCompactedStr, InputFormat.JSON, executionContext -> {
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
