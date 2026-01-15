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

package org.eclipse.edc.heleade.policy.extension.evaluation.participantid;

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
 * Binds specific constraint keys to relevant policy scopes and registers implementations
 * of atomic constraint rule functions for policy enforcement
 * Binds the participant_id constraint key to the contract negotiation scope
 * in a policy enforcement context.
 * must be used as left operand when declaring constraints.
 * rightOperand can be a string.
 * Also supports the IN Operator with a list of string-participant_ids as right operand.
 Example:
 {
 "constraint": {
 "leftOperand": "participant_id",
 "operator": "EQ",
 "rightOperand": "provider-mastral"
 }
 },
 OR
 {
 "constraint": {
 "leftOperand": "participant_id",
 "operator": "IN",
 "rightOperand": "provider-mastral, provider-ebird"}
 }
 */
public class ParticipantIdPolicyExtension implements ServiceExtension {
    static final String NAME = "Participant id Policy Function";
    private static final String PARTICIPANT_ID_KEY = "participant_id";
    static final String PARTICIPANT_ID_CONSTRAINT_KEY = EDC_NAMESPACE + PARTICIPANT_ID_KEY;

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
        monitor.info("Policy function participant id extension initialized");
        registerFunctionAndBindTo(ContractNegotiationPolicyContext.class, ContractNegotiationPolicyContext.NEGOTIATION_SCOPE, monitor);
    }

    private <C extends ParticipantAgentPolicyContext> void registerFunctionAndBindTo(Class<C> contextClass, String scope, Monitor monitor) {
        ruleBindingRegistry.bind(ODRL_USE_ACTION_ATTRIBUTE, scope);
        ruleBindingRegistry.bind(PARTICIPANT_ID_CONSTRAINT_KEY, scope);
        policyEngine.registerFunction(contextClass, Duty.class, PARTICIPANT_ID_CONSTRAINT_KEY, new ParticipantIdPolicyFunction<>(monitor, PARTICIPANT_ID_KEY, fcParticipantClaimChecker));
        policyEngine.registerFunction(contextClass, Permission.class, PARTICIPANT_ID_CONSTRAINT_KEY, new ParticipantIdPolicyFunction<>(monitor, PARTICIPANT_ID_KEY, fcParticipantClaimChecker));
        policyEngine.registerFunction(contextClass, Prohibition.class, PARTICIPANT_ID_CONSTRAINT_KEY, new ParticipantIdPolicyFunction<>(monitor, PARTICIPANT_ID_KEY, fcParticipantClaimChecker));
    }
}