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

package org.eclipse.edc.heleade.federated.catalog.extension.store.mongodb;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.Objects;
import java.util.concurrent.TimeUnit;


/**
 * A base class for MongoDB-based storage implementations, providing utility methods
 * for database operations such as serialization, deserialization, and establishing connections.
 */
public class MongodbStore {

    /**
     * Context for managing database transaction boundaries.
     * Final field initialized during construction.
     */
    protected final TransactionContext transactionContext;
    private final String dataSourceUri;
    private final String dataSourceDb;
    private final ObjectMapper objectMapper;

    /**
     * Constructs a new MongodbStore instance with the specified parameters.
     *
     * @param dataSourceUri the URI of the MongoDB data source to connect to
     * @param dataSourceDb the name of the MongoDB database to use
     * @param transactionContext the transaction context to manage database transactions
     * @param objectMapper the object mapper for handling JSON serialization and deserialization
     */
    public MongodbStore(String dataSourceUri, String dataSourceDb, TransactionContext transactionContext, ObjectMapper objectMapper) {
        this.dataSourceUri = Objects.requireNonNull(dataSourceUri);
        this.dataSourceDb = Objects.requireNonNull(dataSourceDb);
        this.transactionContext = Objects.requireNonNull(transactionContext);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    /**
     * Creates a connection to a MongoDB database using a connection URI string
     *
     * @return A MongoClient instance representing the connection
     */
    protected MongoClient getConnection()  {
        ServerApi serverApi = ServerApi.builder()
                .version(ServerApiVersion.V1)
                .build();
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(dataSourceUri))
                .applyToSocketSettings(builder ->
                        builder.connectTimeout(30, TimeUnit.SECONDS)
                                .readTimeout(30, TimeUnit.SECONDS))
                .applyToClusterSettings(builder ->
                        builder.serverSelectionTimeout(30, TimeUnit.SECONDS))
                .retryWrites(true)
                .retryReads(true)
                .serverApi(serverApi)
                .build();
        // Create a new client and connect to the server
        try {
            return MongoClients.create(settings);
        } catch (Exception e) {
            throw new EdcPersistenceException(e);
        }
    }

    /**
     * Retrieves a specific MongoDB collection from a MongoDB database associated with the provided connection.
     *
     * @param connection the MongoClient instance used to establish the database connection
     * @return the MongoCollection representing the "edc_federated_catalog" collection in the configured database
     */
    protected MongoCollection<Document> getCollection(MongoClient connection) {
        MongoDatabase database = connection.getDatabase(dataSourceDb);
        return database.getCollection(getFederatedCatalogCollectionName());
    }

    /**
     * Converts the provided object into its JSON string representation.
     *
     * @param object the object to be converted to JSON. If the object is null, the method returns null.
     * @return a JSON string representation of the provided object. If the object is a String,
     *         the same string is returned. If the object cannot be serialized, an EdcPersistenceException is thrown.
     * @throws EdcPersistenceException if an error occurs during the JSON serialization process.
     */
    protected String toJson(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return object instanceof String ? object.toString() : objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new EdcPersistenceException(e);
        }
    }

    /**
     * Deserializes a JSON string into an object of the specified type.
     *
     * @param json the JSON string to deserialize; can be null
     * @param type the class of the object to deserialize the JSON string into
     * @param <T> the type of the object to be returned
     * @return an object of type T deserialized from the JSON string, or null if the input JSON string is null
     * @throws EdcPersistenceException if the JSON string cannot be deserialized into the specified type
     */
    protected <T> T fromJson(String json, Class<T> type) {

        if (json == null) {
            return null;
        }

        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new EdcPersistenceException(e);
        }
    }

    /**
     * Provides the name of the MongoDB collection used to store federated catalog data.
     *
     * @return the name of the collection as a String, which is "edc_federated_catalog"
     */
    public static String getFederatedCatalogCollectionName() {
        return "edc_federated_catalog";
    }

    /**
     * Retrieves the name of the identifier field used in the MongoDB store.
     *
     * @return the name of the identifier field, typically "store_id".
     */
    public static String getIdField() {
        return "store_id";
    }


    /**
     * Returns the name of the field representing a marked entity or record.
     *
     * @return the string "marked", indicating the field name.
     */
    public static String getMarkedField() {
        return "marked";
    }

}
