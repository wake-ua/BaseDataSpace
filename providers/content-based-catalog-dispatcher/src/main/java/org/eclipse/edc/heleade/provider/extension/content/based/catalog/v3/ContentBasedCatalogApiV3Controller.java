/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.heleade.provider.extension.content.based.catalog.v3;

import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import org.eclipse.edc.connector.controlplane.api.management.catalog.BaseCatalogApiController;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogService;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.heleade.provider.extension.content.based.catalog.dispatcher.ContentBasedCatalogApiPaths.CBM_BASE_PATH;
import static org.eclipse.edc.heleade.provider.extension.content.based.catalog.dispatcher.ContentBasedCatalogApiPaths.CBM_CATALOG_REQUEST;

/**
 * Controller class for handling API requests specific to the Content-Based Catalog functionality (version 3).
 * Extends the BaseCatalogApiController and implements the ContentBasedCatalogApiV3 interface.
 */
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v3" + CBM_BASE_PATH)
public class ContentBasedCatalogApiV3Controller extends BaseCatalogApiController implements ContentBasedCatalogApiV3 {
    /**
     * Constructs a ContentBasedCatalogApiV3Controller with the specified dependencies.
     *
     * @param service the catalog service to handle business logic for catalog operations
     * @param transformerRegistry the registry to handle type transformations
     * @param validatorRegistry the registry to validate JSON objects
     */
    public ContentBasedCatalogApiV3Controller(CatalogService service, TypeTransformerRegistry transformerRegistry, JsonObjectValidatorRegistry validatorRegistry) {
        super(service, transformerRegistry, validatorRegistry);
    }

    @POST
    @Path(CBM_CATALOG_REQUEST)
    @Override
    public void requestCatalogV3(JsonObject request, @Suspended AsyncResponse response) {
        requestCatalog(request, response);
    }

}
