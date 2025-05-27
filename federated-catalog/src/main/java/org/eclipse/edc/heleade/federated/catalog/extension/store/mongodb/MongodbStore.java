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
     * Constructs a new instance of MongodbStore, initializing it with the specified parameters.
     *
     * @param dataSourceUri       the connection URI for the data source; must not be null
     * @param dataSourceDb        the name of the database to use; must not be null
     * @param transactionContext  the context for handling transactions; must not be null
     * @param objectMapper        the ObjectMapper instance for JSON serialization and deserialization; must not be null
     */
    public MongodbStore(String dataSourceUri, String dataSourceDb, TransactionContext transactionContext, ObjectMapper objectMapper) {
        this.dataSourceUri = Objects.requireNonNull(dataSourceUri);
        this.dataSourceDb = Objects.requireNonNull(dataSourceDb);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.transactionContext = Objects.requireNonNull(transactionContext);
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
     * Retrieves a MongoDB collection from the specified database connection.
     *
     * @param connection      the MongoClient instance representing the connection to the MongoDB server; must not be null
     * @param collectionName  the name of the collection to retrieve; must not be null
     * @return the MongoCollection representing the specified collection in the MongoDB database
     */
    protected MongoCollection<Document> getCollection(MongoClient connection, String collectionName) {
        MongoDatabase database = connection.getDatabase(dataSourceDb);
        return database.getCollection(collectionName);
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
}
