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

package org.eclipse.edc.heleade.federated.catalog.extension.api.node.directory;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.crawler.spi.TargetNode;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
import org.eclipse.edc.heleade.federated.catalog.extension.store.mongodb.node.directory.MongodbFederatedCatalogNodeDirectory;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;

import java.util.ArrayList;
import java.util.List;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * The CatalogNodeController is a REST controller providing endpoints to interact with a federated catalog.
 * It exposes functionalities to retrieve the directory of target nodes in the catalog.
 */
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/v1alpha/directory")
public class NodeDirectoryController {

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
    public NodeDirectoryController(Monitor monitor, TargetNodeDirectory targetNodeDirectory, JsonLd jsonLd) {

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
    public String getDirectory() {
        JsonArray directoryJson = targetNodeDirectory.getAll().stream()
                .map(this::convertToJsonObject)
                .collect(toJsonArray());

        return directoryJson.toString();
    }

    /**
     * Adds a new node to the target node directory by converting the input JsonObject to a TargetNode
     * and then storing it in the directory. The added node is then converted back to a JsonObject for the response.
     *
     * @param node the JsonObject representation of the target node to be added
     * @return the JsonObject representation of the added target node
     */
    @POST
    public JsonObject addNode(JsonObject node) {
        TargetNode targetNode = convertToTargetNode(node);
        targetNodeDirectory.insert(targetNode);
        return convertToJsonObject(targetNode);
    }

    /**
     * Deletes a target node from the federated catalog node directory using its unique identifier.
     * If the underlying target node directory supports MongoDB, the specified entry will be removed.
     * Otherwise, an {@link UnsupportedOperationException} will be thrown.
     *
     * @param id the unique identifier of the node to be deleted
     * @throws UnsupportedOperationException if the target node directory does not support deletion
     */
    @DELETE
    @Path("{id}")
    public void delete(@PathParam("id") String id) {
        if (targetNodeDirectory instanceof MongodbFederatedCatalogNodeDirectory) {
            try {
                MongodbFederatedCatalogNodeDirectory mongodbDirectory = (MongodbFederatedCatalogNodeDirectory) targetNodeDirectory;
                mongodbDirectory.delete(id);
            } catch (EdcPersistenceException e) {
                throw new WebApplicationException("Node not found", Response.Status.NOT_FOUND);
            }
        } else {
            throw new UnsupportedOperationException("The target node directory does not support deletion");
        }
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

    /**
     * Converts a JsonObject into a TargetNode object.
     *
     * @param jsonObject the JsonObject to convert
     * @return a TargetNode representation of the JsonObject
     * @throws RuntimeException if there's an error during conversion
     */
    private TargetNode convertToTargetNode(JsonObject jsonObject) {
        try {
            // First, expand the JSON object if it's in compact form
            JsonObject expanded = jsonLd.expand(jsonObject).getContent();

            // Extract values from the expanded JSON
            String name = getStringValue(expanded, EDC_NAMESPACE + "name");
            String id = getStringValue(expanded, EDC_NAMESPACE + "id");
            String url = getStringValue(expanded, EDC_NAMESPACE + "url");
            List<String> protocols = getStringArrayValues(expanded, EDC_NAMESPACE + "supportedProtocols");

            // Create and return a new TargetNode
            return new TargetNode(name, id, url, protocols);
        } catch (Exception e) {
            throw new RuntimeException("Error converting JsonObject to TargetNode", e);
        }
    }

    /**
     * Helper method to extract a string value from a JSON object.
     */
    private String getStringValue(JsonObject jsonObject, String key) {
        if (jsonObject.containsKey(key)) {
            var valueArray = jsonObject.getJsonArray(key);
            if (valueArray != null && !valueArray.isEmpty()) {
                var firstValue = valueArray.get(0);
                if (firstValue instanceof JsonObject valueObj && valueObj.containsKey("@value")) {
                    return valueObj.getString("@value");
                }
            }
        }
        return null;
    }

    /**
     * Helper method to extract string array values from a JSON object.
     */
    private List<String> getStringArrayValues(JsonObject jsonObject, String key) {
        List<String> results = new ArrayList<>();

        if (jsonObject.containsKey(key)) {
            var valueArray = jsonObject.getJsonArray(key);
            if (valueArray != null) {
                for (var i = 0; i < valueArray.size(); i++) {
                    var item = valueArray.get(i);
                    if (item instanceof JsonObject itemObj) {
                        // Handle case where array items are objects with @value
                        if (itemObj.containsKey("@value")) {
                            results.add(itemObj.getString("@value"));
                        } else if (itemObj.containsKey("@list")) {
                            // Handle case where the array is represented as @list
                            var listArray = itemObj.getJsonArray("@list");
                            for (var j = 0; j < listArray.size(); j++) {
                                var listItem = listArray.get(j);
                                if (listItem instanceof JsonObject listItemObj && listItemObj.containsKey("@value")) {
                                    results.add(listItemObj.getString("@value"));
                                }
                            }
                        }
                    } else if (item.getValueType().toString().equals("STRING")) {
                        // Simple string value
                        results.add(item.toString().replace("\"", ""));
                    }
                }
            }
        }
        return results;
    }
}
