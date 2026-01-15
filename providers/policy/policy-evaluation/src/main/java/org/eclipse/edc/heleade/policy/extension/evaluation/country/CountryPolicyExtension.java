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

package org.eclipse.edc.heleade.policy.extension.evaluation.country;

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

import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_USE_ACTION_ATTRIBUTE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Extension responsible for initializing and registering policy-related functionality.
 * - Binds specific constraint keys to relevant policy scopes and registers implementations
 * of atomic constraint rule functions for policy enforcement
 * - Binds the referring connector constraint key to the contract negotiation scope and catalog scope
 * in a policy enforcement context.
 ----------------------------------------------------------
 * The key for referring connector country constrain.
 * Must be used as left operand when declaring constraints country
 * rightOperand can be a string-3-letter-code country
 * Also supports the IN Operator with a list of string-3-letter-code country as right operand. Example: esp,prt
 * Example:
 *
 <pre>
 *   {
 *     "constraint": {
 *         "leftOperand": "country",
 *         "operator": "EQ",
 *         "rightOperand": "esp"
 *     }
 *  }
 * </pre>
 **
 */
public class CountryPolicyExtension  implements ServiceExtension {
    static final String NAME = "Country constraint Policy Function";
    private static final String COUNTRY_KEY = "country";
    static final String COUNTRY_CONSTRAINT_KEY = EDC_NAMESPACE + COUNTRY_KEY;

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
        monitor.info("Policy function country extension initialized");
        registerFunctionAndBindTo(ContractNegotiationPolicyContext.class, ContractNegotiationPolicyContext.NEGOTIATION_SCOPE, monitor);
    }

    private <C extends ParticipantAgentPolicyContext> void registerFunctionAndBindTo(Class<C> contextClass, String scope, Monitor monitor) {
        ruleBindingRegistry.bind(ODRL_USE_ACTION_ATTRIBUTE, scope);
        ruleBindingRegistry.bind(COUNTRY_CONSTRAINT_KEY, scope);
        policyEngine.registerFunction(contextClass, Duty.class, COUNTRY_CONSTRAINT_KEY, new CountryPolicyFunction<>(monitor, COUNTRY_KEY, fcParticipantClaimChecker));
        policyEngine.registerFunction(contextClass, Permission.class, COUNTRY_CONSTRAINT_KEY, new CountryPolicyFunction<>(monitor, COUNTRY_KEY, fcParticipantClaimChecker));
        policyEngine.registerFunction(contextClass, Prohibition.class, COUNTRY_CONSTRAINT_KEY, new CountryPolicyFunction<>(monitor, COUNTRY_KEY, fcParticipantClaimChecker));
    }


}
