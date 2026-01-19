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
 *       MO - Universidad de Alicante - initial implementation
 *
 */

package org.eclipse.edc.heleade.commons.verification.claims.checker;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 *  * Represents the result of a verification process including signature and claim validation.
 *
 * @param signatureResult Indicates the result of the signature validation process.
 *                        This value is true if the signature validation was successful,
 *                        otherwise false if the signature validation failed.
 * @param claimsResult    Indicates whether the claim validation process was successful.
 *                        This result reflects the evaluation of the claims provided by
 *                        a participant against a set of expected conditions or standards.
 */
public record VerificationResult(boolean signatureResult, boolean claimsResult, boolean success, String message) {

    private static final String SIGNATURE_RESULT = EDC_NAMESPACE + "signatureResult";
    private static final String CLAIMS_RESULT = EDC_NAMESPACE + "claimsResult";
    private static final String SUCCESS = EDC_NAMESPACE + "success";
    private static final String MESSAGE = EDC_NAMESPACE + "message";

    /**
     * Represents the result of a verification process, including the validation
     * of both signature and claims.
     *
     * @param signatureResult Indicates the result of the signature validation process.
     *                        This value is true if the signature validation was successful,
     *                        otherwise false if the signature validation failed.
     * @param claimsResult    Indicates whether the claim validation process was successful.
     *                        This result reflects the evaluation of the claims provided by
     *                        a participant against a set of expected conditions or standards.
     *
     * @param success         Combination of signature and claims result
     *
     * @param message         Text explaining validation failure reasons
     */
    public VerificationResult {
    }

    /**
     * Converts the current instance into a JsonObject representation containing its properties.
     *
     * @return a JsonObject containing the attributes of the instance
     */
    public JsonObject asJsonObject() {
        try {
            // Create a JSON object with the TargetNode properties
            JsonObjectBuilder builder = Json.createObjectBuilder()
                    .add(SIGNATURE_RESULT, this.signatureResult())
                    .add(CLAIMS_RESULT, this.claimsResult())
                    .add(SUCCESS, this.claimsResult() && this.signatureResult())
                    .add(MESSAGE, this.message());

            // Build and return the JSON object
            JsonObject jsonObject = builder.build();

            return jsonObject;

        } catch (Exception e) {
            throw new RuntimeException("Error converting VerificationResult to JsonObject", e);
        }
    }

    /**
     * Creates an instance from a JsonObject representation containing its properties.
     *
     * @param jsonObject a JsonObject containing the attributes of the instance
     * @return a VerificationResult containing the attributes of the instance
     */
    public static VerificationResult fromJsonObject(JsonObject jsonObject) {
        try {
            boolean signatureResult = jsonObject.getBoolean(EDC_NAMESPACE + "signatureResult");
            boolean claimsResult = jsonObject.getBoolean(CLAIMS_RESULT);
            boolean success = jsonObject.getBoolean(SUCCESS);
            String message = jsonObject.getString(MESSAGE);
            return new VerificationResult(signatureResult, claimsResult, success, message);

        } catch (Exception e) {
            throw new RuntimeException("Error converting JsonObject to VerificationResult", e);
        }
    }
}
