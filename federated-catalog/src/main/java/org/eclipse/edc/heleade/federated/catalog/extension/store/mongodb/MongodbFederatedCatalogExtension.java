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
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
import org.eclipse.edc.heleade.federated.catalog.extension.store.mongodb.cache.MongodbFederatedCatalogCache;
import org.eclipse.edc.heleade.federated.catalog.extension.store.mongodb.node.directory.MongodbFederatedCatalogNodeDirectory;
import org.eclipse.edc.jsonld.JsonLdExtension;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCAT_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCAT_SCHEMA;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCT_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCT_SCHEMA;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_PREFIX;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * This class is an implementation of a ServiceExtension providing a MongoDB-based Federated Catalog Cache.
 * It defines configurations and initializes the required components for managing and interacting with a
 * federated catalog stored in a MongoDB database.
 */
@Provides({FederatedCatalogCache.class, TargetNodeDirectory.class})
@Extension(value = "MongoDB federated catalog extension")
public class MongodbFederatedCatalogExtension implements ServiceExtension {
    private static final String FEDERATED_CATALOG_URI_PROPERTY = "org.eclipse.edc.heleade.federated.catalog.extension.store.mongodb.uri";
    private static final String FEDERATED_CATALOG_URI_DEFAULT = "mongodb://localhost:27017/";
    private static final String FEDERATED_CATALOG_DB_PROPERTY = "org.eclipse.edc.heleade.federated.catalog.extension.store.mongodb.db";
    private static final String FEDERATED_CATALOG_DB_DEFAULT = "federatedcatalogdb";

    private Monitor monitor;
    private String dataSourceUri;
    private String dataSourceDb;
    private JsonLd jsonLd;
    private MongodbFederatedCatalogNodeDirectory catalogNodeDirectory;

    @Inject
    private DataSourceRegistry dataSourceRegistry;
    @Inject
    private TransactionContext trxContext;
    @Inject
    private TypeManager typeManager;
    @Inject
    private TypeTransformerRegistry transformerRegistry;

    /**
     * Provides the TargetNodeDirectory component configured for use with the federated catalog.
     *
     * @return an instance of TargetNodeDirectory representing the node directory for the federated catalog.
     */
    @Provider
    public TargetNodeDirectory federatedCacheNodeDirectory() {
        return this.catalogNodeDirectory;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        typeManager.registerTypes(Catalog.class, Dataset.class);
        dataSourceUri = context.getConfig().getString(FEDERATED_CATALOG_URI_PROPERTY, FEDERATED_CATALOG_URI_DEFAULT);
        dataSourceDb = context.getConfig().getString(FEDERATED_CATALOG_DB_PROPERTY, FEDERATED_CATALOG_DB_DEFAULT);
        monitor = context.getMonitor();

        jsonLd = new JsonLdExtension().createJsonLdService(context);
        jsonLd.registerNamespace(VOCAB, EDC_NAMESPACE);
        jsonLd.registerNamespace(DCAT_PREFIX, DCAT_SCHEMA);
        jsonLd.registerNamespace(DCT_PREFIX, DCT_SCHEMA);
        jsonLd.registerNamespace(DSPACE_PREFIX, DSPACE_SCHEMA);
        jsonLd.registerNamespace(ODRL_PREFIX, ODRL_SCHEMA);
        jsonLd.registerNamespace(VOCAB, EDC_NAMESPACE);
        jsonLd.registerNamespace(ODRL_PREFIX, ODRL_SCHEMA);

        var store = new MongodbFederatedCatalogCache(dataSourceUri, dataSourceDb, trxContext, typeManager.getMapper(), jsonLd, transformerRegistry);
        monitor.info("MongoDB Cache Store Ready");
        context.registerService(FederatedCatalogCache.class, store);

        this.catalogNodeDirectory = new MongodbFederatedCatalogNodeDirectory(dataSourceUri, dataSourceDb, trxContext, typeManager.getMapper());
        monitor.info("MongoDB Node Directory Store Ready");
        context.registerService(TargetNodeDirectory.class, this.catalogNodeDirectory);
    }
}
