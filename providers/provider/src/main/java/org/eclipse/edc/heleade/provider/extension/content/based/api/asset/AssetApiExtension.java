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

package org.eclipse.edc.heleade.provider.extension.content.based.api.asset;

import org.eclipse.edc.api.validation.DataAddressValidator;
import org.eclipse.edc.connector.controlplane.api.management.asset.validation.AssetValidator;
import org.eclipse.edc.connector.controlplane.services.spi.asset.AssetService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_ASSET_TYPE;
import static org.eclipse.edc.spi.types.domain.DataAddress.EDC_DATA_ADDRESS_TYPE;

/**
 * Provides initialization and registration of the Asset Management API extension
 * specifically for content-based assets.
 * This extension integrates with the service runtime to register custom asset API controllers,
 * validators, and transformers for processing content-based assets. It is designed to
 * support the creation, update, and management of assets via JSON-LD representations.
 */
@Extension(value = AssetApiExtension.NAME)
public class AssetApiExtension implements ServiceExtension {

    /**
     * Represents the name of the Content-Based Management API for handling asset-related operations.
     * This constant is used to identify the Asset Management API extension focused on content-based assets.
     */
    public static final String NAME = "Content Based Management API: Asset";

    @Inject
    private WebService webService;

    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Inject
    private AssetService assetService;

    @Inject
    private JsonObjectValidatorRegistry validator;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        validator.register(EDC_ASSET_TYPE, AssetValidator.instance());
        validator.register(EDC_DATA_ADDRESS_TYPE, DataAddressValidator.instance());

        var managementTypeTransformerRegistry = transformerRegistry.forContext("management-api");

        managementTypeTransformerRegistry.register(new CbmJsonObjectToJsonObjectAssetTransformer());

        webService.registerResource(ApiContext.MANAGEMENT, new ContentBasedAssetApiController(assetService,
                managementTypeTransformerRegistry, monitor, validator));
    }
}
