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

package org.eclipse.edc.heleade.provider.extension.content.based.catalog.dispatcher;

import org.eclipse.edc.protocol.dsp.catalog.http.dispatcher.CatalogApiPaths;

/**
 * Defines API paths for the content-based catalog in the Dataspace Protocol framework.
 * Extends the base catalog API paths and introduces specific paths for content-based catalogs.
 */
public interface ContentBasedCatalogApiPaths extends CatalogApiPaths {
    /**
     * Base path for the content-based catalog API in the Dataspace Protocol framework (/catalog-cbm).
     * Extends the base catalog path with a specific suffix for content-based catalog functionality.
     */
    String CBM_BASE_PATH = CatalogApiPaths.BASE_PATH + "-cbm";
    /**
     * Represents the API path for a content-based catalog request in the Dataspace Protocol framework (/request-cbm).
     * Combines the base catalog request path with a specific suffix for content-based catalog functionality.
     */
    String CBM_CATALOG_REQUEST = CatalogApiPaths.CATALOG_REQUEST + "-cbm";
}
