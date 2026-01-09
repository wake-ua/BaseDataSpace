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

package org.eclipse.edc.heleade.policy.extension.claims.checker;

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
public record VerificationResult(boolean signatureResult, boolean claimsResult) {
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
     */
    public VerificationResult {
    }
}
