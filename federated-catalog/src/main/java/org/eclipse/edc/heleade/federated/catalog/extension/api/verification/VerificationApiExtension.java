/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.heleade.federated.catalog.extension.api.verification;

import org.eclipse.edc.catalog.spi.FederatedCatalogCache;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
import org.eclipse.edc.jsonld.JsonLdExtension;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.web.spi.WebService;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * The VerificationApiExtension class is a service extension responsible for initializing
 * and registering the API for verification within a federated catalog system.
 * It integrates various components, such as JSON-LD handling, logging, and APIs for
 * participant and claims verification.
 *
 * This extension configures the JSON-LD context, registers required namespaces,
 * and initializes a REST controller for handling verification requests.
 *
 * Implements the ServiceExtension interface to participate in the service lifecycle.
 */
public class VerificationApiExtension implements ServiceExtension {

    @Inject
    private TargetNodeDirectory targetNodeDirectory;

    @Inject
    private TypeManager typeManager;

    @Inject
    WebService webService;

    @Inject
    private FederatedCatalogCache store;

    private JsonLd jsonLd;
    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        jsonLd = new JsonLdExtension().createJsonLdService(context);
        jsonLd.registerNamespace(VOCAB, EDC_NAMESPACE);

        webService.registerResource(new VerificationApiController(monitor, typeManager, targetNodeDirectory, jsonLd));
    }
}
