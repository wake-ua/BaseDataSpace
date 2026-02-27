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

package org.eclipse.edc.heleade.federated.catalog.extension.content.based;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

/**
 * Provides an extension for a content-based federated catalog cache within the Eclipse Data Space
 * Connector. Registers and initializes transformers to process datasets for the federated catalog
 * using a content-driven approach.
 */
@Extension(value = FederatedCatalogContentBasedExtension.NAME)
public class FederatedCatalogContentBasedExtension implements ServiceExtension {

    /**
     * A constant representing the name of the content-based federated catalog cache extension.
     * Used for identifying and naming the extension in the Eclipse Data Space Connector.
     */
    public static final String NAME = "Content Based Federated Catalog Cache";

    @Inject
    private Monitor monitor;
    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor.info("Initializing Content Based Federated Catalog Extension");
        registerTransformers(context);
    }

    private void registerTransformers(ServiceExtensionContext context) {
        transformerRegistry.register(new JsonObjectToDatasetContentBasedTransformer());
    }
}
