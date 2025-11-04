/*
 *  Copyright (c) 2024 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.policy.extension;

import org.eclipse.edc.connector.controlplane.catalog.spi.policy.CatalogPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.ContractNegotiationPolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import static org.eclipse.edc.connector.controlplane.catalog.spi.policy.CatalogPolicyContext.CATALOG_SCOPE;
import static org.eclipse.edc.connector.controlplane.contract.spi.policy.ContractNegotiationPolicyContext.NEGOTIATION_SCOPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_USE_ACTION_ATTRIBUTE;
import static org.eclipse.edc.policy.engine.spi.PolicyEngine.ALL_SCOPES;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Extension responsible for initializing and registering policy-related functionality.
 * Binds specific constraint keys to relevant policy scopes and registers implementations
 * of atomic constraint rule functions for policy enforcement
 * Key features:
 * - Binds the location constraint key to the contract negotiation scope.
 * - Binds the entityType constraint key to the contract catalog scope.
 * - Registers the {@link LocationConstraintFunction} to evaluate location-based constraints
 * - Registers the {@link EntityConstraintFunction} to evaluate entity-based constraints
 * in a policy enforcement context.
 */
public class PolicyFunctionsExtension implements ServiceExtension {
    private static final String LOCATION_KEY = "location";
    private static final String ENTITY_KEY = "entityType";
    private static final String LOCATION_CONSTRAINT_KEY = EDC_NAMESPACE + LOCATION_KEY;
    private static final String ENTITY_CONSTRAINT_KEY = EDC_NAMESPACE + ENTITY_KEY;

    @Inject
    private RuleBindingRegistry ruleBindingRegistry;
    @Inject
    private PolicyEngine policyEngine;

    @Override
    public String name() {
        return "Heleade policy functions";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();
        var participantId = context.getParticipantId();
        var participantRegistryUrl = context.getConfig().getString("edc.participant.registry.url");
        var participantRegistryApiKey = context.getConfig().getString("edc.participant.registry.apikey");

        ParticipantClaimChecker claimChecker = new FcParticipantClaimChecker(monitor, participantRegistryUrl, participantRegistryApiKey);

        ruleBindingRegistry.bind(ODRL_USE_ACTION_ATTRIBUTE, ALL_SCOPES);
        ruleBindingRegistry.bind(LOCATION_CONSTRAINT_KEY, NEGOTIATION_SCOPE);
        ruleBindingRegistry.bind(ENTITY_CONSTRAINT_KEY, CATALOG_SCOPE);

        policyEngine.registerFunction(ContractNegotiationPolicyContext.class, Permission.class, LOCATION_CONSTRAINT_KEY,
                new LocationConstraintFunction(monitor, participantId, LOCATION_KEY, claimChecker));

        policyEngine.registerFunction(CatalogPolicyContext.class,
                Permission.class, ENTITY_CONSTRAINT_KEY, new EntityConstraintFunction(monitor, participantId, ENTITY_KEY, claimChecker));
    }
}

