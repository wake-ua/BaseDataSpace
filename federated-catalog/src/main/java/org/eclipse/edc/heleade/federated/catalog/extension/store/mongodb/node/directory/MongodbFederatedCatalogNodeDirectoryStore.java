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
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.edc.crawler.spi.TargetNode;
import org.eclipse.edc.heleade.federated.catalog.extension.api.node.directory.ParticipantNode;
import org.eclipse.edc.heleade.federated.catalog.extension.store.mongodb.MongodbStore;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.ArrayList;
import java.util.Arrays;
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
    public List<TargetNode> queryAllTargetNodes() {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var collection = getCollection(connection, getFederatedCatalogNodeDirectoryCollectionName());
                var findClause = new Document("url", new Document("$exists", true))
                        .append("supportedProtocols", new Document("$not", new Document("$size", 0L)))
                        .append("$expr", new Document("$gt", Arrays.asList(new Document("$strLenCP", "$url"), 0L)));

                var nodes = collection.find(findClause).into(new ArrayList<>());
                return nodes.stream()
                        .map(doc -> fromJson(doc.toJson(), TargetNode.class))
                        .toList();
            }
        });
    }

    /**
     * Retrieves a list of all {@code ParticipantNode} entries from the federated catalog node directory.
     * This method uses a transactional context to interact with the MongoDB collection, deserializing
     * the result into {@code ParticipantNode} objects.
     *
     * @return a list of {@code ParticipantNode} objects representing the entries in the federated catalog node directory
     */
    public List<ParticipantNode> queryAllParticipantNodes() {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var collection = getCollection(connection, getFederatedCatalogNodeDirectoryCollectionName());
                var nodes = collection.find().into(new ArrayList<>());
                return nodes.stream()
                        .map(doc -> fromJson(doc.toJson(), ParticipantNode.class))
                        .toList();
            }
        });
    }

    /**
     * Persists the given {@code TargetNode} to the federated catalog node directory collection in MongoDB.
     * This method uses a transactional context for atomic operations, converts the {@code TargetNode}
     * to its JSON representation, and upserts it as a document into the database.
     *
     * @param node the {@code TargetNode} to be saved in the MongoDB collection; must not be null
     */
    public void save(TargetNode node) {
        save(new ParticipantNode(node));
    }

    /**
     * Persists the given {@code TargetNode} to the federated catalog node directory collection in MongoDB.
     * This method uses a transactional context for atomic operations, converts the {@code TargetNode}
     * to its JSON representation, and upserts it as a document into the database.
     *
     * @param node the {@code TargetNode} to be saved in the MongoDB collection; must not be null
     */
    public void save(ParticipantNode node) {
        transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                upsertInternal(connection, node);
            } catch (Exception e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    /**
     * Deletes an entry from the federated catalog node directory in MongoDB by its unique identifier.
     * This method uses a transactional context to ensure the operation is performed atomically.
     *
     * @param id the unique identifier of the entry to be deleted; must not be null
     */
    public void delete(String id) {
        transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                deleteInternal(connection, id);
            } catch (Exception e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    private void deleteInternal(MongoClient connection, String id) {
        Bson filter = Filters.eq(getIdField(), id);
        MongoCollection<Document> collection = getCollection(connection, getFederatedCatalogNodeDirectoryCollectionName());
        DeleteResult result = collection.deleteOne(filter);
        if (result.getDeletedCount() < 1) {
            throw new EdcPersistenceException("No node found for id " + id);
        }
    }

    private void upsertInternal(MongoClient connection, ParticipantNode node) {
        Bson filter = Filters.eq(getIdField(), node.id());
        UpdateOptions options = new UpdateOptions().upsert(true);
        Document catalogDoc = Document.parse(toJson(node));
        Bson update = new Document("$set", catalogDoc);
        MongoCollection<Document> collection = getCollection(connection, getFederatedCatalogNodeDirectoryCollectionName());
        collection.updateOne(filter, update, options);
    }

    /**
     * Retrieves the name of the MongoDB collection used for storing federated catalog node directory entries.
     *
     * @return the name of the MongoDB collection as a string
     */
    public static String getFederatedCatalogNodeDirectoryCollectionName() {
        return "edc_node_directory";
    }

    /**
     * Retrieves the name of the identifier field used in the MongoDB store.
     *
     * @return the name of the identifier field, typically "store_id".
     */
    public static String getIdField() {
        return "id";
    }
}
