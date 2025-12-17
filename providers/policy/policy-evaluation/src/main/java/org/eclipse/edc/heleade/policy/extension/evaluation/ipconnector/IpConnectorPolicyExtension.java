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

package org.eclipse.edc.heleade.policy.extension.evaluation.ipconnector;

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
 * - Binds specific constraint keys to relevant policy scopes and registers implementations
 * of atomic constraint rule functions for policy enforcement
 * - Binds the referring connector constraint key to the contract negotiation scope and catalog scope
 * in a policy enforcement context.
 ----------------------------------------------------------
 * The key for referring connector constraints.
 * Must be used as left operand when declaring constraints.
 * rightOperand can be a string-IP or a comma separated list of string-IPS.
 * Also supports the IN Operator with a list of string-IPS as right operand.
 * Example:
 *
 <pre>
 *   {
 *     "constraint": {
 *         "leftOperand": "ip_connector",
 *         "operator": "EQ",
 *         "rightOperand": "219.208.53.217,219.208.53.219"
 *     }
 *  }
 * </pre>
 **
 */
public class IpConnectorPolicyExtension  implements ServiceExtension {
    static final String NAME = "Ip Connector Policy";
    private static final String IP_CONNECTOR_KEY = "ip_connector";
    static final String IP_CONNECTOR_CONSTRAINT_KEY = EDC_NAMESPACE + IP_CONNECTOR_KEY;

    @Inject
    private RuleBindingRegistry ruleBindingRegistry;
    @Inject
    private PolicyEngine policyEngine;

    @Inject
    FcParticipantClaimChecker fcParticipantClaimChecker;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();
        monitor.info("Policy function referring connector extension initialized");
        registerFunctionAndBindTo(ContractNegotiationPolicyContext.class, ContractNegotiationPolicyContext.NEGOTIATION_SCOPE, monitor);
    }

    private <C extends ParticipantAgentPolicyContext> void registerFunctionAndBindTo(Class<C> contextClass, String scope, Monitor monitor) {
        ruleBindingRegistry.bind(ODRL_USE_ACTION_ATTRIBUTE, scope);
        ruleBindingRegistry.bind(IP_CONNECTOR_CONSTRAINT_KEY, scope);
        policyEngine.registerFunction(contextClass, Duty.class, IP_CONNECTOR_CONSTRAINT_KEY, new IpConnectorPolicyFunction<>(monitor, IP_CONNECTOR_KEY, fcParticipantClaimChecker));
        policyEngine.registerFunction(contextClass, Permission.class, IP_CONNECTOR_CONSTRAINT_KEY, new IpConnectorPolicyFunction<>(monitor, IP_CONNECTOR_KEY, fcParticipantClaimChecker));
        policyEngine.registerFunction(contextClass, Prohibition.class, IP_CONNECTOR_CONSTRAINT_KEY, new IpConnectorPolicyFunction<>(monitor, IP_CONNECTOR_KEY, fcParticipantClaimChecker));
    }


}
