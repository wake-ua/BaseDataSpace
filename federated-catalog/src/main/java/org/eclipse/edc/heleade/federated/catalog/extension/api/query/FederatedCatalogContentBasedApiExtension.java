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

package org.eclipse.edc.heleade.federated.catalog.extension.api.query;

import jakarta.json.Json;
import org.eclipse.edc.catalog.spi.QueryService;
import org.eclipse.edc.connector.core.agent.NoOpParticipantIdMapper;
import org.eclipse.edc.heleade.federated.catalog.extension.content.based.JsonObjectFromDatasetContentBasedTransformer;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.protocol.dsp.catalog.transform.from.JsonObjectFromCatalogTransformer;
import org.eclipse.edc.protocol.dsp.catalog.transform.from.JsonObjectFromDataServiceTransformer;
import org.eclipse.edc.protocol.dsp.catalog.transform.from.JsonObjectFromDistributionTransformer;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.apiversion.ApiVersionService;
import org.eclipse.edc.spi.system.health.HealthCheckService;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.PortMappingRegistry;

import java.util.Map;

import static org.eclipse.edc.catalog.spi.FccApiContexts.CATALOG_QUERY;
import static org.eclipse.edc.heleade.commons.content.based.catalog.CbmConstants.CBM_PREFIX;
import static org.eclipse.edc.heleade.commons.content.based.catalog.CbmConstants.CBM_SCHEMA;
import static org.eclipse.edc.heleade.commons.content.based.catalog.CbmConstants.RDF_NAMESPACE;
import static org.eclipse.edc.heleade.commons.content.based.catalog.CbmConstants.RDF_PREFIX;
import static org.eclipse.edc.heleade.commons.content.based.catalog.CbmConstants.SCHEMA_PREFIX;
import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.SCHEMA_ORG_NAMESPACE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCAT_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCAT_SCHEMA;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCT_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCT_SCHEMA;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_PREFIX;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_PREFIX;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

/**
 * Provides an implementation of the ServiceExtension interface, enabling the
 * Federated Catalog Content-Based API functionality. This extension facilitates
 * querying and retrieving cached federated catalog content using content-based search
 * criteria, leveraging JSON-LD and type transformation mechanisms.
 *
 * It integrates with various services including:
 * - WebService: To register API endpoints.
 * - QueryService: To query catalog data.
 * - TypeTransformerRegistry: To handle data transformations.
 * - ApiVersionService: To manage versioning of the API.
 *
 * Registers JSON-LD namespaces and transformers required for catalog data manipulation
 * and enables the extension through the implemented initialization logic.
 */
@Extension(value = FederatedCatalogContentBasedApiExtension.NAME)
public class FederatedCatalogContentBasedApiExtension implements ServiceExtension {

    /**
     * Represents the name of the content-based cache query API extension.
     * Used as an identifier for the Federated Catalog Content-Based API extension.
     */
    public static final String NAME = "Content Based Cache Query API Extension";
    static final String CATALOG_QUERY_SCOPE = "CATALOG_QUERY_API";


    @Inject
    private WebService webService;
    @Inject
    private QueryService queryService;
    @Inject(required = false)
    private HealthCheckService healthCheckService;
    @Inject
    private JsonLd jsonLd;
    @Inject
    private TypeManager typeManager;
    @Inject
    private TypeTransformerRegistry transformerRegistry;
    @Inject
    private ApiVersionService apiVersionService;
    @Inject
    private PortMappingRegistry portMappingRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {

        jsonLd.registerNamespace(VOCAB, EDC_NAMESPACE, CATALOG_QUERY_SCOPE);
        jsonLd.registerNamespace(EDC_PREFIX, EDC_NAMESPACE, CATALOG_QUERY_SCOPE);
        jsonLd.registerNamespace(ODRL_PREFIX, ODRL_SCHEMA, CATALOG_QUERY_SCOPE);
        jsonLd.registerNamespace(DCAT_PREFIX, DCAT_SCHEMA, CATALOG_QUERY_SCOPE);
        jsonLd.registerNamespace(DCT_PREFIX, DCT_SCHEMA, CATALOG_QUERY_SCOPE);
        jsonLd.registerNamespace(DSPACE_PREFIX, DSPACE_SCHEMA, CATALOG_QUERY_SCOPE);
        jsonLd.registerNamespace(CBM_PREFIX, CBM_SCHEMA, CATALOG_QUERY_SCOPE);
        jsonLd.registerNamespace(RDF_PREFIX, RDF_NAMESPACE, CATALOG_QUERY_SCOPE);
        jsonLd.registerNamespace(SCHEMA_PREFIX, SCHEMA_ORG_NAMESPACE, CATALOG_QUERY_SCOPE);

        var catalogController = new FederatedCatalogContentBasedApiController(queryService, transformerRegistry);
        webService.registerResource(CATALOG_QUERY, catalogController);

        var jsonFactory = Json.createBuilderFactory(Map.of());
        var mapper = context.getService(TypeManager.class).getMapper(JSON_LD);
        var participantIdMapper = new NoOpParticipantIdMapper();
        transformerRegistry.register(new JsonObjectFromCatalogTransformer(jsonFactory, typeManager, JSON_LD, participantIdMapper));
        transformerRegistry.register(new JsonObjectFromDatasetContentBasedTransformer(jsonFactory, typeManager, JSON_LD));
        transformerRegistry.register(new JsonObjectFromDistributionTransformer(jsonFactory));
        transformerRegistry.register(new JsonObjectFromDataServiceTransformer(jsonFactory));
    }


}
