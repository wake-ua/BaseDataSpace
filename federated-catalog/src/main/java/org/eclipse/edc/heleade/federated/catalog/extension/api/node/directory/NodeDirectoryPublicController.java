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

import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
import org.eclipse.edc.heleade.federated.catalog.extension.store.mongodb.node.directory.MongodbFederatedCatalogNodeDirectory;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;

/**
 * The CatalogNodeController is a REST controller providing endpoints to interact with a federated catalog.
 * It exposes functionalities to retrieve the directory of target nodes in the catalog.
 */
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/directory")
public class NodeDirectoryPublicController {

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
    public NodeDirectoryPublicController(Monitor monitor, TargetNodeDirectory targetNodeDirectory, JsonLd jsonLd) {

        this.monitor = monitor;
        this.targetNodeDirectory = targetNodeDirectory;
        this.jsonLd = jsonLd;
    }

    /**
     * Retrieves a participant node as a JSON object based on the provided unique identifier.
     *
     * @param id the unique identifier of the participant node to be retrieved; must not be null or blank
     * @return the JSON object representation of the participant node corresponding to the given identifier
     * @throws WebApplicationException if the id is invalid, the node is not found, or an error occurs while querying the node
     * @throws UnsupportedOperationException if the target node directory does not support querying by id
     */
    @GET
    @Path("{id}")
    public String getNode(@PathParam("id") String id) {
        if (id == null || id.isBlank()) {
            throw new WebApplicationException("Invalid null or blank node id", Response.Status.BAD_REQUEST);
        }

        if (targetNodeDirectory instanceof MongodbFederatedCatalogNodeDirectory) {
            try {
                MongodbFederatedCatalogNodeDirectory mongodbDirectory = (MongodbFederatedCatalogNodeDirectory) targetNodeDirectory;
                ParticipantNode participantNode = mongodbDirectory.getParticipantNode(id);
                if (participantNode == null) {
                    throw new WebApplicationException("Node not found", Response.Status.NOT_FOUND);
                }

                return convertToPublicJsonObject(participantNode).toString();

            } catch (EdcPersistenceException e) {
                throw new WebApplicationException("Node not found", Response.Status.NOT_FOUND);
            }
        } else {
            throw new UnsupportedOperationException("The target node directory does not support query by id");
        }
    }

    /**
     * Converts a ParticipantNode object into a JsonObject.
     *
     * @param node the ParticipantNode to convert
     * @return a JsonObject representation of the ParticipantNode
     * @throws RuntimeException if there's an error during JSON conversion
     */
    private JsonObject convertToPublicJsonObject(ParticipantNode node) {
        JsonObject publicNodeJson = jsonLd.compact(node.asPublicJsonObject()).getContent();
        monitor.info(publicNodeJson.toString());
        return publicNodeJson;
    }
}
