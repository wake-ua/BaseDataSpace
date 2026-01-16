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

package org.eclipse.edc.heleade.federated.catalog.extension.api.verification;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
import org.eclipse.edc.heleade.federated.catalog.extension.api.node.directory.ParticipantNode;
import org.eclipse.edc.heleade.federated.catalog.extension.store.mongodb.node.directory.MongodbFederatedCatalogNodeDirectory;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.TypeManager;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.eclipse.edc.heleade.commons.verification.claims.Claims.verifyClaims;
import static org.eclipse.edc.heleade.commons.verification.claims.Claims.verifySignature;


/**
 * REST API controller for managing verification operations.
 * Handles JSON-LD based signature and claims verification for participants.
 */
@Consumes({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/verification")
public class VerificationApiController {

    private final JsonLd jsonLd;
    private final Monitor monitor;
    private final TypeManager typeManager;
    private final MongodbFederatedCatalogNodeDirectory targetNodeDirectory;

    /**
     * Constructor for the VerificationApiController.
     * Initializes the controller with the required dependencies for handling verification operations.
     *
     * @param monitor the Monitor instance used for logging and diagnostics
     * @param typeManager the TypeManager instance for managing data types within the system
     * @param targetNodeDirectory the TargetNodeDirectory implementation managing the federated catalog nodes
     * @param jsonLd the JsonLd instance for handling JSON-LD operations
     */
    public VerificationApiController(Monitor monitor, TypeManager typeManager, TargetNodeDirectory targetNodeDirectory, JsonLd jsonLd) {
        this.monitor = monitor;
        this.jsonLd = jsonLd;
        this.typeManager = typeManager;
        this.targetNodeDirectory = (MongodbFederatedCatalogNodeDirectory) targetNodeDirectory;
    }

    /**
     * Verifies the provided claims by checking the participant's signature and matching claims.
     *
     * @param body a {@link String} containing the participant ID, signed claims, and claims to verify.
     *               Expected keys include "participantId", "participantSignedClaims", and "participantClaims".
     * @return a {@link JsonObject} containing the results of the verification process, including:
     *         - "verifySignatureSuccess": a boolean indicating if the signature verification was successful.
     *         - "verifyClaimsSuccess": a boolean indicating if the claims validation was successful.
     *         - "success": a boolean indicating the overall success of the verification process.
     */
    @POST
    public JsonObject verify(String body) {

        // get the json object from the string
        JsonReader jsonReader = Json.createReader(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
        JsonObject jsonBody = jsonReader.readObject();
        jsonReader.close();

        // get the inputs from the body
        String id = jsonBody.getString("participantId");
        String participantSignedClaims = jsonBody.getString("signedClaims");
        Map<String, Object> participantClaims = getMapFromJsonObject(jsonBody.getJsonObject("claims"));
        JsonObject participantClaimsJson = jsonBody.getJsonObject("claims");
        String claimsString = participantClaimsJson.toString();

        // check the signature
        ParticipantNode participantNode = targetNodeDirectory.getParticipantNode(id);
        String pem = participantNode.security().get("https://w3id.org/edc/v0.0.1/ns/pem");
        boolean verifySignatureSuccess = verifySignature(typeManager.getMapper(), pem, participantSignedClaims, claimsString);
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("verifySignatureSuccess", verifySignatureSuccess);

        // check the claims
        Map<String, Object> participantClaimsFromFc = participantNode.claims();
        boolean verifyClaimsSuccess = verifyClaims(participantClaims, participantClaimsFromFc);
        builder.add("verifyClaimsSuccess", verifyClaimsSuccess);

        // return overall result
        builder.add("success", verifySignatureSuccess && verifyClaimsSuccess);
        return builder.build();
    }

    private Map<String, Object> getMapFromJsonObject(JsonObject jsonObject) {
        Map<String, Object> map = new HashMap<String, Object>();
        for (String key : jsonObject.keySet()) {
            Object value = jsonObject.get(key);
            if (value instanceof JsonString) {
                String stringValue = ((JsonString) value).getString();
                map.put(key, stringValue);
            } else if (value instanceof JsonObject) {
                map.put(key, getMapFromJsonObject((JsonObject) value));
            } else {
                throw new IllegalArgumentException("Invalid type for key: " + key);
            }
        }
        return map;
    }
}
