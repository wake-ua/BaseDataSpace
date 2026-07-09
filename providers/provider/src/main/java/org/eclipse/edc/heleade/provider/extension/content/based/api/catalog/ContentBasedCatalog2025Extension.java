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
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.protocol.dsp.catalog.http.api.decorator.Base64continuationTokenSerDes;
import org.eclipse.edc.protocol.dsp.catalog.http.api.decorator.ContinuationTokenManagerImpl;
import org.eclipse.edc.protocol.dsp.catalog.validation.CatalogRequestMessageValidator;
import org.eclipse.edc.protocol.dsp.http.spi.message.ContinuationTokenManager;
import org.eclipse.edc.protocol.dsp.http.spi.message.DspRequestHandler;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.protocol.spi.ProtocolWebhookResolver;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.jersey.providers.jsonld.JerseyJsonLdInterceptor;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

import java.util.Optional;

import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DATASPACE_PROTOCOL_HTTP_V_2025_1;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DSP_NAMESPACE_V_2025_1;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DSP_SCOPE_V_2025_1;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DSP_TRANSFORMER_CONTEXT_V_2025_1;
import static org.eclipse.edc.protocol.dsp.spi.type.DspCatalogPropertyAndTypeNames.DSPACE_TYPE_CATALOG_REQUEST_MESSAGE_TERM;
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
@Extension(value = ContentBasedCatalog2025Extension.NAME)
public class ContentBasedCatalog2025Extension implements ServiceExtension {

    /**
     * A constant representing the name of the Content Based Catalog Extension.
     */
    public static final String NAME = "Dataspace Protocol Content Based Catalog Extension";

    @Inject
    private WebService webService;
    @Inject
    private CatalogProtocolService service;
    @Inject
    private DataServiceRegistry dataServiceRegistry;
    @Inject
    private DspRequestHandler dspRequestHandler;
    @Inject
    private TypeTransformerRegistry transformerRegistry;
    @Inject
    private DataspaceProfileContextRegistry dataspaceProfileContextRegistry;
    @Inject
    private CriterionOperatorRegistry criterionOperatorRegistry;
    @Inject
    private JsonObjectValidatorRegistry validatorRegistry;
    @Inject
    private ProtocolWebhookResolver protocolWebhookResolver;
    @Inject
    private SingleParticipantContextSupplier participantContextSupplier;
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
        registerValidators();

        webService.registerResource(ApiContext.PROTOCOL, new ContentBasedCatalogApiController20251(service, dspRequestHandler,
                continuationTokenManager(monitor, DSP_TRANSFORMER_CONTEXT_V_2025_1, DSP_NAMESPACE_V_2025_1), participantContextSupplier, monitor));
        webService.registerDynamicResource(ApiContext.PROTOCOL, ContentBasedCatalogApiController20251.class, new JerseyJsonLdInterceptor(jsonLd, typeManager, JSON_LD, DSP_SCOPE_V_2025_1));

        monitor.info("Content Based Catalog Extension initialized");
    }

    private ContinuationTokenManager continuationTokenManager(Monitor monitor, String version, JsonLdNamespace namespace) {
        var continuationTokenSerDes = new Base64continuationTokenSerDes(transformerRegistry.forContext(version), jsonLd);
        return new ContinuationTokenManagerImpl(continuationTokenSerDes, namespace, monitor);
    }

    @Override
    public void prepare() {
        registerDataService();
        monitor.info("Content Based Catalog Extension prepared");
    }

    @Override
    public void start() {
        monitor.info("Content Based Catalog Extension started");
    }

    private void registerDataService() {
        // TODO: @Lucia review this class
        dataServiceRegistry.register(DATASPACE_PROTOCOL_HTTP_V_2025_1, this::createDataService);
    }

    private DataService createDataService(String participantContextId, String protocol) {
        return Optional.ofNullable(protocolWebhookResolver.getWebhook(participantContextId, protocol))
                .map(webhook -> DataService.Builder.newInstance()
                        .endpointDescription("dspace:connector")
                        .endpointUrl(webhook.url())
                        .build()).orElse(null);
    }

    private void registerValidators() {
        validatorRegistry.register(DSP_NAMESPACE_V_2025_1.toIri(DSPACE_TYPE_CATALOG_REQUEST_MESSAGE_TERM), CatalogRequestMessageValidator.instance(criterionOperatorRegistry, DSP_NAMESPACE_V_2025_1));
    }
}
