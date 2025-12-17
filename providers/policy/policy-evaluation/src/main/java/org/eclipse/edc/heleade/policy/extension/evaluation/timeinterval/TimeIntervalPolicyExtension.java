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
 *       MO - Universidad de Alicante - initial implementation
 *
 */

package org.eclipse.edc.heleade.policy.extension.evaluation.timeinterval;

import org.eclipse.edc.connector.controlplane.contract.spi.policy.ContractNegotiationPolicyContext;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.time.OffsetDateTime;

import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_USE_ACTION_ATTRIBUTE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Extension responsible for initializing and registering policy-related functionality.
 * Binds specific constraint keys to relevant policy scopes and registers implementations
 * of atomic constraint rule functions for policy enforcement
 * Key features:
 * Binds the policy_evaluation_time constraint key to the contract negotiation scope,
 * catalog scope, and transfer scope
 * in a policy enforcement context.
 * The key for policy_evaluation_time.
 * must be used as left operand when declaring constraints.
 * rightOperand should be a ISO 8601 timestamp such as 2025-07-20T12:34:56Z
 * Supported operators: LT, LEQ, GT, GEQ, EQ, NEQ
 * * Example:
 *  *
 *  <pre>
 *  *   {
 *  *     "constraint": {
 *  *         "leftOperand": "policy_evaluation_time",
 *  *         "operator": "EQ",
 *  *         "rightOperand": "2025-07-20T12:34:56Z"
 *  *     }
 *  *  }
 *  * </pre>
 */
public class TimeIntervalPolicyExtension implements ServiceExtension {

    static final String NAME = "Time Interval Policy";
    static final String POLICY_EVALUATION_TIME_CONSTRAINT_KEY = EDC_NAMESPACE + "policy_evaluation_time";

    @Inject
    private RuleBindingRegistry ruleBindingRegistry;

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
        ruleBindingRegistry.bind(POLICY_EVALUATION_TIME_CONSTRAINT_KEY, scope);
        policyEngine.registerFunction(contextClass, Permission.class, POLICY_EVALUATION_TIME_CONSTRAINT_KEY, new TimeIntervalPolicyFunction<>(OffsetDateTime::now, monitor));
    }

}

