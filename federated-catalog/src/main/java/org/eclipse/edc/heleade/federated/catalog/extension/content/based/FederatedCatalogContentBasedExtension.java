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

import jakarta.json.Json;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import java.util.Map;

import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(value = org.eclipse.edc.catalog.cache.FederatedCatalogCacheExtension.NAME)
public class FederatedCatalogContentBasedExtension implements ServiceExtension {

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

        var jsonFactory = Json.createBuilderFactory(Map.of());
        var mapper = context.getService(TypeManager.class).getMapper(JSON_LD);
        //        transformerRegistry.register(new JsonObjectFromDatasetContentBasedTransformer(jsonFactory, mapper));
        //        transformerRegistry.register(new JsonObjectFromDistributionContentBasedTransformer(jsonFactory));
    }
}
