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

import org.eclipse.edc.catalog.spi.FederatedCatalogCache;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

/**
 * This extension integrates a MongoDB-based implementation of a federated catalog cache into the
 * Eclipse Dataspace Connector (EDC) framework. It registers the necessary components and
 * configuration settings for the federated catalog cache to interact with a MongoDB database.
 *
 * The provided federated catalog cache implementation supports storing and retrieving catalog
 * data using MongoDB as the underlying database. MongoDB connection parameters, such as
 * the URI and database name, are configurable through application settings.
 *
 * Key configuration properties:
 * - {@code org.eclipse.edc.heleade.federated.catalog.extension.store.mongodb.uri}:
 *   Specifies the connection URI for the MongoDB database. If not provided, defaults to
 *   {@code mongodb://localhost:27017/}.
 * - {@code org.eclipse.edc.heleade.federated.catalog.extension.store.mongodb.db}:
 *   Specifies the name of the MongoDB database to use. If not provided, defaults to
 *   {@code federatedcatalogdb}.
 *
 * This extension utilizes the following capabilities:
 * - {@code DataSourceRegistry}: Service to manage data source configurations.
 * - {@code TransactionContext}: Manages transactional operations for database interactions.
 * - {@code TypeManager}: Registers and manages data type mappings for catalog and dataset instances.
 *
 * Responsibilities:
 * - Configures MongoDB connection settings from the application context.
 * - Registers the {@code Catalog} and {@code Dataset} data types with the {@code TypeManager}.
 * - Initializes and registers the {@code FederatedCatalogCache} service implementation backed
 *   by MongoDB.
 * - Uses the provided monitor for logging and diagnostic purposes.
 *
 * Implements:
 * - {@link ServiceExtension}: EDC framework interface for extensions.
 */
@Provides(FederatedCatalogCache.class)
@Extension(value = "MongoDB federated catalog cache")
public class MongodbFederatedCatalogCacheExtension implements ServiceExtension {
    private static final String FEDERATED_CATALOG_URI_PROPERTY = "org.eclipse.edc.heleade.federated.catalog.extension.store.mongodb.uri";
    private static final String FEDERATED_CATALOG_URI_DEFAULT = "mongodb://localhost:27017/";
    private static final String FEDERATED_CATALOG_DB_PROPERTY = "org.eclipse.edc.heleade.federated.catalog.extension.store.mongodb.db";
    private static final String FEDERATED_CATALOG_DB_DEFAULT = "federatedcatalogdb";

    private Monitor monitor;
    private String dataSourceUri;
    private String dataSourceDb;

    @Inject
    private DataSourceRegistry dataSourceRegistry;
    @Inject
    private TransactionContext trxContext;
    @Inject
    private TypeManager typeManager;

    @Override
    public void initialize(ServiceExtensionContext context) {
        typeManager.registerTypes(Catalog.class, Dataset.class);
        dataSourceUri = context.getConfig().getString(FEDERATED_CATALOG_URI_PROPERTY, FEDERATED_CATALOG_URI_DEFAULT);
        dataSourceDb = context.getConfig().getString(FEDERATED_CATALOG_DB_PROPERTY, FEDERATED_CATALOG_DB_DEFAULT);
        monitor = context.getMonitor();

        var store = new MongodbFederatedCatalogCache(dataSourceUri, dataSourceDb, trxContext, typeManager.getMapper());
        context.registerService(FederatedCatalogCache.class, store);
    }
}
