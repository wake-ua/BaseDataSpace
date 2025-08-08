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

import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.protocol.dsp.http.dispatcher.PostDspHttpRequestFactory;
import org.eclipse.edc.protocol.dsp.http.serialization.ByteArrayBodyExtractor;
import org.eclipse.edc.protocol.dsp.http.spi.dispatcher.DspHttpRemoteMessageDispatcher;
import org.eclipse.edc.protocol.dsp.http.spi.dispatcher.DspRequestBasePathProvider;
import org.eclipse.edc.protocol.dsp.http.spi.serialization.JsonLdRemoteMessageSerializer;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import static org.eclipse.edc.heleade.provider.extension.content.based.catalog.dispatcher.ContentBasedCatalogApiPaths.CBM_BASE_PATH;
import static org.eclipse.edc.heleade.provider.extension.content.based.catalog.dispatcher.ContentBasedCatalogApiPaths.CBM_CATALOG_REQUEST;

/**
 * Creates and registers the HTTP dispatcher delegate for sending a catalog request as defined in
 * the dataspace protocol specification.
 */
@Extension(value = DspContentBasedCatalogHttpDispatcherExtension.NAME)
public class DspContentBasedCatalogHttpDispatcherExtension implements ServiceExtension {

    /**
     * The name of the extension responsible for creating and registering the HTTP dispatcher delegate
     * for handling content-based catalog requests as per the Dataspace Protocol specification.
     */
    public static final String NAME = "Dataspace Protocol Content Based Catalog HTTP Dispatcher Extension";

    @Inject
    private DspHttpRemoteMessageDispatcher messageDispatcher;
    @Inject
    private JsonLdRemoteMessageSerializer remoteMessageSerializer;
    @Inject
    private DspRequestBasePathProvider dspRequestBasePathProvider;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var byteArrayBodyExtractor = new ByteArrayBodyExtractor();

        messageDispatcher.registerMessage(
                CatalogRequestMessage.class,
                new PostDspHttpRequestFactory<>(remoteMessageSerializer, dspRequestBasePathProvider, m -> CBM_BASE_PATH + CBM_CATALOG_REQUEST),
                byteArrayBodyExtractor
        );
    }

    public void prepare() {
        var byteArrayBodyExtractor = new ByteArrayBodyExtractor();

        messageDispatcher.registerMessage(
                CatalogRequestMessage.class,
                new PostDspHttpRequestFactory<>(remoteMessageSerializer, dspRequestBasePathProvider, m -> CBM_BASE_PATH + CBM_CATALOG_REQUEST),
                byteArrayBodyExtractor
        );

    }
}
