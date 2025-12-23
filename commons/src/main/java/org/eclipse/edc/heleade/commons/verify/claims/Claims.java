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

package org.eclipse.edc.heleade.commons.verify.claims;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;


public class Claims {

    /**
     * Verifies the signature of the participant's claims using the provided public key.
     *
     * @param mapper                  the ObjectMapper instance for serializing and deserializing JSON data. TypeManager can be used for this purpose.
     * @param pem                     the string representation of the participant's public key in PEM format, this will be extracted from fc
     * @param participantSignedClaims the Base64-encoded signature of the participant's claims
     * @param participantClaims       the map containing the claims provided by the participant
     * @return true if the signature is valid
     */
    public static boolean verifySignature(ObjectMapper mapper,  String pem, String participantSignedClaims, Map<String, Object> participantClaims) {
        try {
            pem = pem.replaceAll("-+[A-Z ]+-+", "")
                    .replaceAll("\\s", "");

            byte[] decoded = Base64.getDecoder().decode(pem);


            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
            KeyFactory keyFactory = KeyFactory.getInstance("Ed25519");

            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            String claimsString = mapper.writeValueAsString(participantClaims);

            Signature signature = Signature.getInstance("Ed25519");
            signature.initVerify(publicKey);
            signature.update(claimsString.getBytes(StandardCharsets.UTF_8));

            byte[] signedBytes = Base64.getDecoder().decode(participantSignedClaims);

            return signature.verify(signedBytes);


        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Verifies if the specified claim in the participant's claims matches the provided verified claim.
     *
     * @param verifiedClaim      the value of the claim to verify against. This is gotten from FC
     * @param participantClaims  the map containing claims provided by the participant
     * @param claimKey           the key of the claim to verify
     * @return true if the verified claim matches the participant's claim for the specified key, false otherwise
     */
    public static boolean verifyClaim(String verifiedClaim, Map<String, Object> participantClaims, String claimKey) {
        Object claimToVerify = participantClaims.get(claimKey);
        return Objects.equals(verifiedClaim, claimToVerify);
    }
}

