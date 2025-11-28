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

package org.eclipse.edc.heleade.policy.extension.evaluation.location;

import org.eclipse.edc.connector.controlplane.catalog.spi.policy.CatalogPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.ContractNegotiationPolicyContext;
import org.eclipse.edc.heleade.policy.extension.claims.checker.FcParticipantClaimChecker;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_USE_ACTION_ATTRIBUTE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Extension responsible for initializing and registering policy-related functionality.
 * Binds specific constraint keys to relevant policy scopes and registers implementations
 * of atomic constraint rule functions for policy enforcement
 * Key features:
 * - Binds the location constraint key to the contract negotiation scope and catalog scope
 * in a policy enforcement context.
 */
public class LocationPolicyExtension implements ServiceExtension {
    private static final String LOCATION_KEY = "location";
    private static final String LOCATION_CONSTRAINT_KEY = EDC_NAMESPACE + LOCATION_KEY;

    @Inject
    private RuleBindingRegistry ruleBindingRegistry;
    @Inject
    private PolicyEngine policyEngine;

    @Inject
    FcParticipantClaimChecker fcParticipantClaimChecker;

    @Override
    public String name() {
        return "Policy function location extension";
    }


    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();
        monitor.info("Policy function location extension initialized");
        registerFunctionAndBindTo(CatalogPolicyContext.class, CatalogPolicyContext.CATALOG_SCOPE, monitor);
        registerFunctionAndBindTo(ContractNegotiationPolicyContext.class, ContractNegotiationPolicyContext.NEGOTIATION_SCOPE, monitor);
    }

    private <C extends ParticipantAgentPolicyContext> void registerFunctionAndBindTo(Class<C> contextClass, String scope, Monitor monitor) {
        ruleBindingRegistry.bind(ODRL_USE_ACTION_ATTRIBUTE, scope);
        ruleBindingRegistry.bind(LOCATION_CONSTRAINT_KEY, scope);
        policyEngine.registerFunction(contextClass, Duty.class, LOCATION_CONSTRAINT_KEY, new LocationPolicyFunction<>(monitor, LOCATION_KEY, fcParticipantClaimChecker));
        policyEngine.registerFunction(contextClass, Permission.class, LOCATION_CONSTRAINT_KEY, new LocationPolicyFunction<>(monitor, LOCATION_KEY, fcParticipantClaimChecker));
        policyEngine.registerFunction(contextClass, Prohibition.class, LOCATION_CONSTRAINT_KEY, new LocationPolicyFunction<>(monitor, LOCATION_KEY, fcParticipantClaimChecker));
    }

}

