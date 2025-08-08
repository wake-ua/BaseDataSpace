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

package org.eclipse.edc.heleade.provider.extension.content.based.catalog.v3;

import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

/**
 * The ContentBasedCatalogApiExtension serves as a service extension implementation that integrates
 * a content-based catalog management API into the control plane. It registers a management API controller
 * to handle catalog-related operations.
 *
 * This extension initializes required services such as the {@link WebService}, {@link TypeTransformerRegistry},
 * {@link CatalogService}, and {@link JsonObjectValidatorRegistry}, and registers the API controller that
 * processes requests to the v3 content-based catalog endpoint.
 */
@Extension(value = org.eclipse.edc.connector.controlplane.api.management.catalog.CatalogApiExtension.NAME)
public class ContentBasedCatalogApiExtension implements ServiceExtension {

    /**
     * The constant representing the name of the Content Based Catalog Management API.
     * Used as an identifier for the extension integrating the catalog functionality
     * into the control plane's management API.
     */
    public static final String NAME = "Management API: Content Based Catalog";

    @Inject
    private WebService webService;

    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Inject
    private CatalogService service;

    @Inject
    private JsonObjectValidatorRegistry validatorRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {

        var managementApiTransformerRegistry = transformerRegistry.forContext("management-api");
        webService.registerResource(ApiContext.MANAGEMENT, new ContentBasedCatalogApiV3Controller(service, managementApiTransformerRegistry, validatorRegistry));

    }
}
