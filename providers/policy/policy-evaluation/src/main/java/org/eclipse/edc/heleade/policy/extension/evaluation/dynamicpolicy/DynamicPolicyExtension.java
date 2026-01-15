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

package org.eclipse.edc.heleade.policy.extension.evaluation.dynamicpolicy;

import org.eclipse.edc.connector.controlplane.contract.spi.policy.ContractNegotiationPolicyContext;
import org.eclipse.edc.heleade.commons.verification.claims.checker.FcParticipantClaimChecker;
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

import java.util.Set;

import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_USE_ACTION_ATTRIBUTE;

/**
 * The {@code DynamicPolicyExtension} class implements the {@link ServiceExtension} interface
 * and provides dynamic policy evaluation by registering specific policy functions with
 * the {@link PolicyEngine}. These functions dynamically resolve policy constraints
 * tied to participant claims and orchestrate their evaluation for use in contract negotiations.
 *
 * This extension registers policy evaluation functions for ODRL (Open Digital Rights
 * Language) rules such as {@link Permission}, {@link Duty}, and {@link Prohibition}.
 * It dynamically binds these functions to a specific scope within the
 * {@link RuleBindingRegistry} to enforce policy constraints during contract negotiations.
 *
 * Features:
 * - Registers evaluation functions with the {@link PolicyEngine} for managing
 *   ODRL constraints using claim-driven verification.
 * - Binds rules to specific negotiation scopes dynamically.
 * - Utilizes {@link FcParticipantClaimChecker} for verifying participant claims
 *   against the participant registry.
 *
 * Dependencies:
 * - {@link RuleBindingRegistry}: Handles the registration and binding of rules.
 * - {@link PolicyEngine}: Executes the policy functions for evaluating constraints.
 * - {@link FcParticipantClaimChecker}: Verifies participant claim validity.
 * - {@link Monitor}: Logs and monitors system behavior.
 */
public class DynamicPolicyExtension implements ServiceExtension {

    static final String NAME = "Dynamic Policy Extension";

    @Inject
    private RuleBindingRegistry ruleBindingRegistry;

    @Inject
    FcParticipantClaimChecker fcParticipantClaimChecker;

    @Inject
    private PolicyEngine policyEngine;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();
        registerFunctionAndBindTo(ContractNegotiationPolicyContext.class, ContractNegotiationPolicyContext.NEGOTIATION_SCOPE, monitor);
    }

    private <C extends ParticipantAgentPolicyContext> void registerFunctionAndBindTo(Class<C> contextClass, String scope, Monitor monitor) {
        ruleBindingRegistry.bind(ODRL_USE_ACTION_ATTRIBUTE, scope);
        ruleBindingRegistry.dynamicBind((ruletype) -> Set.of(scope));
        policyEngine.registerFunction(contextClass, Permission.class, new DynamicPolicyFunction<>(monitor, fcParticipantClaimChecker));
        policyEngine.registerFunction(contextClass, Duty.class, new DynamicPolicyFunction<>(monitor, fcParticipantClaimChecker));
        policyEngine.registerFunction(contextClass, Prohibition.class, new DynamicPolicyFunction<>(monitor, fcParticipantClaimChecker));

    }



}
