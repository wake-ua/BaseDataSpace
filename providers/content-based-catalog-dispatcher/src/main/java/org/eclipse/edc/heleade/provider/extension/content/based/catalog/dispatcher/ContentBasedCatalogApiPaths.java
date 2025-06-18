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

public interface ContentBasedCatalogApiPaths extends CatalogApiPaths {
    String CBM_BASE_PATH = CatalogApiPaths.BASE_PATH + "-cbm";
    String CBM_CATALOG_REQUEST = CatalogApiPaths.CATALOG_REQUEST + "-cbm";
}
