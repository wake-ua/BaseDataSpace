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

package org.eclipse.edc.heleade.federated.catalog.extension.store.mongodb.node.directory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import org.eclipse.edc.crawler.spi.TargetNode;
import org.eclipse.edc.heleade.federated.catalog.extension.store.mongodb.MongodbStore;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.ArrayList;
import java.util.List;

/**
 * MongodbFederatedCatalogNodeDirectoryStore provides a storage implementation
 * for managing federated catalog node directory entries using MongoDB as the
 * backend. It is responsible for performing CRUD operations for TargetNode
 * entities while utilizing transaction handling through the TransactionContext.
 *
 * This class extends the {@link MongodbStore}, inheriting capabilities for
 * MongoDB connection management, document serialization/deserialization, and
 * interaction with collections.
 */
public class MongodbFederatedCatalogNodeDirectoryStore extends MongodbStore {

    /**
     * Constructs a new instance of MongodbFederatedCatalogNodeDirectoryStore for managing
     * federated catalog node directory entries within a MongoDB database. The store handles
     * CRUD operations and transaction management for TargetNode entities.
     *
     * @param dataSourceUri The URI of the MongoDB data source; must not be null.
     * @param dataSourceDb The name of the target MongoDB database; must not be null.
     * @param transactionContext The context used for managing database transactions; must not be null.
     * @param objectMapper The ObjectMapper instance used for JSON serialization and deserialization; must not be null.
     */
    public MongodbFederatedCatalogNodeDirectoryStore(String dataSourceUri, String dataSourceDb, TransactionContext transactionContext, ObjectMapper objectMapper) {
        super(dataSourceUri, dataSourceDb, transactionContext, objectMapper);
    }

    /**
     * Retrieves a list of all {@code TargetNode} entries from the federated catalog node directory.
     * This method uses a transactional context to interact with the MongoDB collection, deserializing
     * the result into {@code TargetNode} objects.
     *
     * @return a list of {@code TargetNode} objects representing the entries in the federated catalog node directory
     */
    public List<TargetNode> queryAll() {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var collection = getCollection(connection, getFederatedCatalogNodeDirectoryCollectionName());
                var nodes = collection.find().into(new ArrayList<>());
                return nodes.stream()
                        .map(doc -> fromJson(doc.toJson(), TargetNode.class))
                        .toList();
            }
        });
    }

    /**
     * Persists the given {@code TargetNode} to the federated catalog node directory collection in MongoDB.
     * This method uses a transactional context for atomic operations, converts the {@code TargetNode}
     * to its JSON representation, and inserts it as a document into the database.
     *
     * @param node the {@code TargetNode} to be saved in the MongoDB collection; must not be null
     */
    public void save(TargetNode node) {
        transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var collection = getCollection(connection, getFederatedCatalogNodeDirectoryCollectionName());
                var json = toJson(node);
                collection.insertOne(Document.parse(json));
            }
            return null;
        });
    }

    /**
     * Retrieves the name of the MongoDB collection used for storing federated catalog node directory entries.
     *
     * @return the name of the MongoDB collection as a string
     */
    public static String getFederatedCatalogNodeDirectoryCollectionName() {
        return "edc_node_directory";
    }

}
