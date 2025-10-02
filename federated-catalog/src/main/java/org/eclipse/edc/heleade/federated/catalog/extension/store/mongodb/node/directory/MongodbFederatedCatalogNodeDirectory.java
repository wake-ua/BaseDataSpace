/*
 *  Copyright (c) 2024 Fraunhofer-Gesellschaft
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft - initial API and implementation
 *
 */

package org.eclipse.edc.heleade.federated.catalog.extension.store.mongodb.node.directory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.crawler.spi.TargetNode;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
import org.eclipse.edc.heleade.federated.catalog.extension.api.node.directory.ParticipantNode;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.List;

/**
 * The {@code MongodbFederatedCatalogNodeDirectory} class is responsible for handling
 * CRUD operations related to the federated catalog node directory using MongoDB
 * as the underlying storage mechanism. It extends the functionality provided by
 * {@code MongodbFederatedCatalogNodeDirectoryStore} and implements the
 * {@code TargetNodeDirectory} interface.
 *
 * This class provides methods to retrieve all target nodes and insert new ones.
 */
public class MongodbFederatedCatalogNodeDirectory extends MongodbFederatedCatalogNodeDirectoryStore implements TargetNodeDirectory {

    /**
     * Constructor for MongodbFederatedCatalogNodeDirectory.
     * This initializes the directory using MongoDB as the underlying data store, with the specified
     * datasource URI, database name, transaction context, and object mapper for JSON serialization and deserialization.
     *
     * @param dataSourceUri The URI of the MongoDB data source.
     * @param dataSourceDb The name of the MongoDB database to be used.
     * @param transactionContext The transaction context to handle database operations atomically.
     * @param objectMapper The ObjectMapper instance used for JSON processing.
     */
    public MongodbFederatedCatalogNodeDirectory(String dataSourceUri, String dataSourceDb, TransactionContext transactionContext, ObjectMapper objectMapper) {
        super(dataSourceUri, dataSourceDb, transactionContext, objectMapper);
    }

    @Override
    public List<TargetNode> getAll() {
        return queryAllTargetNodes();
    }

    @Override
    public void insert(TargetNode targetNode) {
        save(targetNode);
    }

    /**
     * Retrieves a list of all {@code ParticipantNode} objects from the federated catalog node directory.
     *
     * @return a list of {@code ParticipantNode} instances representing all participant nodes in the directory.
     */
    public List<ParticipantNode> getParticipantNodes() {
        return queryAllParticipantNodes();
    }

    /**
     * Inserts the provided {@code ParticipantNode} into the federated catalog node directory.
     *
     * @param participantNode the {@code ParticipantNode} to be inserted; must not be null
     */
    public void insert(ParticipantNode participantNode) {
        save(participantNode);
    }


}
