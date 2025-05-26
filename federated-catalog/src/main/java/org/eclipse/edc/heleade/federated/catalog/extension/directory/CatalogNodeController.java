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

package org.eclipse.edc.heleade.federated.catalog.extension.directory;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.crawler.spi.TargetNode;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.monitor.Monitor;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * The CatalogNodeController is a REST controller providing endpoints to interact with a federated catalog.
 * It exposes functionalities to retrieve the directory of target nodes in the catalog.
 */
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/v1alpha/directory")
public class CatalogNodeController {

    private final JsonLd jsonLd;
    private final Monitor monitor;
    private final TargetNodeDirectory targetNodeDirectory;

    /**
     * Constructs a new instance of CatalogNodeController.
     *
     * @param monitor the monitor used for logging and diagnostics
     * @param targetNodeDirectory the directory containing target nodes for the federated catalog
     * @param jsonLd the JSON-LD service for handling JSON-LD transformations
     */
    public CatalogNodeController(Monitor monitor, TargetNodeDirectory targetNodeDirectory, JsonLd jsonLd) {

        this.monitor = monitor;
        this.targetNodeDirectory = targetNodeDirectory;
        this.jsonLd = jsonLd;
    }

    /**
     * Defines the directory endpoint for the federated catalog
     *
     * @return JSON response
     */
    @GET
    @Path("")
    public String getDirectory() {
        monitor.info("FederatedCatalog received a get directory request");

        JsonArray directoryJson = targetNodeDirectory.getAll().stream()
                .map(this::convertToJsonObject)
                .collect(toJsonArray());

        return directoryJson.toString();
    }

    /**
     * Converts a TargetNode object into a JsonObject.
     *
     * @param node the TargetNode to convert
     * @return a JsonObject representation of the TargetNode
     * @throws RuntimeException if there's an error during JSON conversion
     */
    private JsonObject convertToJsonObject(TargetNode node) {
        try {
            // Create a JSON object with the TargetNode properties
            JsonObjectBuilder builder = Json.createObjectBuilder()
                    .add(EDC_NAMESPACE + "name", node.name())
                    .add(EDC_NAMESPACE + "id", node.id())
                    .add(EDC_NAMESPACE + "url", node.targetUrl());

            // Add the supported protocols as an array
            JsonArrayBuilder protocolsArray = Json.createArrayBuilder();
            for (String protocol : node.supportedProtocols()) {
                protocolsArray.add(protocol);
            }
            builder.add(EDC_NAMESPACE + "supportedProtocols", protocolsArray);

            // Build and return the JSON object
            JsonObject jsonObject = builder.build();

            return jsonLd.compact(jsonObject).getContent();

        } catch (Exception e) {
            throw new RuntimeException("Error converting TargetNode to JsonObject", e);
        }
    }


}
