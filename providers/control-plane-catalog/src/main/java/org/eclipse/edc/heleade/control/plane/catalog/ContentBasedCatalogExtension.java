/*
 *  Copyright (c) 2025 University of Alicante
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       University of Alicante - Initial implementation
 *
 */

package org.eclipse.edc.heleade.control.plane.catalog;

import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.catalog.spi.DatasetResolver;
import org.eclipse.edc.connector.controlplane.catalog.spi.DistributionResolver;
import org.eclipse.edc.connector.controlplane.catalog.spi.policy.CatalogPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;


/**
 * Service extension that integrates content-based catalog functionality and initializes components
 * for dataset resolution within the catalog framework.
 * <p>
 * Key components:
 * - Asset Index: Manages access and indexing of assets
 * - Policy Definition Store: Handles policy definitions
 * - Contract Definition Store: Manages contract terms and asset associations
 * - Distribution Resolver: Resolves asset distribution mechanisms
 * - Criterion Operator Registry: Enables criteria-based filtering
 * - Policy Engine: Handles policy evaluation
 * <p>
 * The extension registers ContentBasedDatasetResolverImpl with content-based filtering capabilities.
 * It initializes by registering catalog policy context scope and provides a configured DatasetResolver.
 */
@Extension(ContentBasedCatalogExtension.NAME)
public class ContentBasedCatalogExtension implements ServiceExtension {

    /**
     * Represents the name identifier for the {@code ContentBasedCatalogExtension}.
     * This constant serves as a unique name for the extension, providing a reference
     * for identification and registration within the system framework.
     */
    public static final String NAME = "Content Based Catalog Core";

    private Monitor monitor;

    @Inject
    private AssetIndex assetIndex;

    @Inject
    private PolicyDefinitionStore policyDefinitionStore;

    @Inject
    private DistributionResolver distributionResolver;

    @Inject
    private CriterionOperatorRegistry criterionOperatorRegistry;

    @Inject
    private ContractDefinitionStore contractDefinitionStore;

    @Inject
    private PolicyEngine policyEngine;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        policyEngine.registerScope(CatalogPolicyContext.CATALOG_SCOPE, CatalogPolicyContext.class);
        this.monitor = context.getMonitor();
    }

    /**
     * Creates and initializes a new instance of {@code DatasetResolver} configured
     * as a {@code ContentBasedDatasetResolverImpl} for resolving datasets using
     * content-based filtering logic.
     * <p>
     * Initializes dependencies including contract definition resolver, asset index,
     * policy stores and registries needed for dataset resolution.
     *
     * @return A configured {@code DatasetResolver} for content-based dataset resolution
     */
    @Provider
    public DatasetResolver datasetResolver() {
        this.monitor.info("Initializing Dataset Resolver");
        var contractDefinitionResolver = new ContentBasedContractDefinitionResolverImpl(contractDefinitionStore, policyEngine, policyDefinitionStore);
        return new ContentBasedDatasetResolverImpl(contractDefinitionResolver, assetIndex, policyDefinitionStore,
                distributionResolver, criterionOperatorRegistry);
    }

}
