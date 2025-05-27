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

package org.eclipse.edc.heleade.federated.catalog.extension.store.mongodb.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.heleade.federated.catalog.extension.store.mongodb.MongodbStore;
import org.eclipse.edc.transaction.spi.TransactionContext;


/**
 * A base class for MongoDB-based storage implementations, providing utility methods
 * for database operations such as serialization, deserialization, and establishing connections.
 */
public class MongodbFederatedCatalogCacheStore extends MongodbStore {

    /**
     * Constructs a new MongodbStore instance with the specified parameters.
     *
     * @param dataSourceUri the URI of the MongoDB data source to connect to
     * @param dataSourceDb the name of the MongoDB database to use
     * @param transactionContext the transaction context to manage database transactions
     * @param objectMapper the object mapper for handling JSON serialization and deserialization
     */
    public MongodbFederatedCatalogCacheStore(String dataSourceUri, String dataSourceDb, TransactionContext transactionContext, ObjectMapper objectMapper) {
        super(dataSourceUri, dataSourceDb, transactionContext, objectMapper);
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
