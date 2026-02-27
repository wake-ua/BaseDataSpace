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

import org.eclipse.edc.catalog.spi.FederatedCatalogCache;
import org.eclipse.edc.catalog.spi.QueryService;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
import org.eclipse.edc.heleade.federated.catalog.extension.api.query.HeleadeQueryServiceImpl;
import org.eclipse.edc.jsonld.JsonLdExtension;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;


/**
 * The CatalogNodeDirectoryExtension is responsible for initializing and configuring
 * components related to the federated catalog directory, including JSON-LD processing,
 * monitoring, and REST API exposure.
 *
 * This extension sets up the necessary services to allow interaction with the federated
 * catalog, such as managing target nodes and JSON-LD transformation for semantic compatibility.
 *
 * Key responsibilities:
 * 1. Registering a {@link NodeDirectoryController} instance as a REST API endpoint for managing
 *    the federated catalog's directory of target nodes.
 * 2. Initializing and configuring the JSON-LD service for handling semantic data transformations.
 * 3. Leveraging the service context to retrieve and manage dependencies such as {@link TargetNodeDirectory},
 *    {@link TypeManager}, {@link WebService}, and {@link Monitor}.
 */
@Extension(value = NodeDirectoryApiExtension.NAME)
public class NodeDirectoryApiExtension implements ServiceExtension {

    public static final String NAME = "Federated Catalog Node Directory API Extension";
    @Inject
    private TargetNodeDirectory targetNodeDirectory;

    @Inject
    private TypeManager typeManager;

    @Inject
    WebService webService;

    @Inject
    private FederatedCatalogCache store;

    private JsonLd jsonLd;
    private Monitor monitor;

    /**
     * Provides the default implementation of the {@link QueryService} using the {@link HeleadeQueryServiceImpl}.
     * This method initializes and returns a query engine that operates on a federated catalog cache.
     *
     * @return the default {@link QueryService} implementation for querying datasets from the federated catalog
     */
    @Provider
    public QueryService defaultQueryEngine() {
        return new HeleadeQueryServiceImpl(store);
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        jsonLd = new JsonLdExtension().createJsonLdService(context);
        jsonLd.registerNamespace(VOCAB, EDC_NAMESPACE);

        webService.registerResource(ApiContext.MANAGEMENT, new NodeDirectoryController(monitor, targetNodeDirectory, jsonLd));
        webService.registerResource(new NodeDirectoryPublicController(monitor, targetNodeDirectory, jsonLd));
    }

}

