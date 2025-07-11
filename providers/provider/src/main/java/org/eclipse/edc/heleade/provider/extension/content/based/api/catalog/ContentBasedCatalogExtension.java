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

package org.eclipse.edc.heleade.provider.extension.content.based.api.catalog;

import org.eclipse.edc.connector.controlplane.catalog.spi.DataService;
import org.eclipse.edc.connector.controlplane.catalog.spi.DataServiceRegistry;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogProtocolService;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.protocol.dsp.catalog.http.api.decorator.Base64continuationTokenSerDes;
import org.eclipse.edc.protocol.dsp.catalog.http.api.decorator.ContinuationTokenManagerImpl;
import org.eclipse.edc.protocol.dsp.http.spi.message.ContinuationTokenManager;
import org.eclipse.edc.protocol.dsp.http.spi.message.DspRequestHandler;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.protocol.ProtocolWebhook;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.providers.jsonld.JerseyJsonLdInterceptor;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_NAMESPACE_V_08;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_SCOPE_V_08;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_TRANSFORMER_CONTEXT_V_08;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

/**
 * The ContentBasedCatalogExtension class is a service extension that integrates a content-based
 * catalog into the Dataspace Protocol framework. It provides functionality for managing and serving
 * content-based metadata catalogs and registers necessary components to support this integration.
 *
 * The extension ensures that the catalog operations conform to content-based schemas by using
 * custom controllers and APIs for request handling and response generation.
 *
 * This class implements the ServiceExtension interface and uses a dependency injection framework
 * for its components.
 */
public class ContentBasedCatalogExtension implements ServiceExtension {

    /**
     * A constant representing the name of the Content Based Catalog Extension.
     */
    public static final String NAME = "Dataspace Protocol Content Based Catalog Extension";

    @Inject
    private WebService webService;
    @Inject
    private ProtocolWebhook protocolWebhook;
    @Inject
    private CatalogProtocolService service;
    @Inject
    private DataServiceRegistry dataServiceRegistry;
    @Inject
    private DspRequestHandler dspRequestHandler;
    @Inject
    private TypeTransformerRegistry transformerRegistry;
    @Inject
    private Monitor monitor;
    @Inject
    private TypeManager typeManager;
    @Inject
    private JsonLd jsonLd;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var jsonLdMapper = typeManager.getMapper(JSON_LD);

        webService.registerResource(ApiContext.PROTOCOL, new ContentBasedCatalogApiController(service, dspRequestHandler, continuationTokenManager(monitor, DSP_TRANSFORMER_CONTEXT_V_08, DSP_NAMESPACE_V_08), monitor));
        webService.registerDynamicResource(ApiContext.PROTOCOL, ContentBasedCatalogApiController.class, new JerseyJsonLdInterceptor(jsonLd, jsonLdMapper, DSP_SCOPE_V_08));


        dataServiceRegistry.register(DataService.Builder.newInstance()
                .endpointDescription("dspace:connector")
                .endpointUrl(protocolWebhook.url())
                .build());
    }

    private ContinuationTokenManager continuationTokenManager(Monitor monitor, String version, JsonLdNamespace namespace) {
        var continuationTokenSerDes = new Base64continuationTokenSerDes(transformerRegistry.forContext(version), jsonLd);
        return new ContinuationTokenManagerImpl(continuationTokenSerDes, namespace, monitor);
    }

}
