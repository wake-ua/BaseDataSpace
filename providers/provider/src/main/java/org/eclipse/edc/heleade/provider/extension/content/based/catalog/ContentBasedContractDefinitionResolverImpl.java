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

package org.eclipse.edc.heleade.provider.extension.content.based.catalog;

import org.eclipse.edc.connector.controlplane.catalog.spi.ContractDefinitionResolver;
import org.eclipse.edc.connector.controlplane.catalog.spi.ResolvedContractDefinitions;
import org.eclipse.edc.connector.controlplane.catalog.spi.policy.CatalogPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;

import java.util.HashMap;
import java.util.Optional;


/**
 * Determines the contract definitions applicable to a {@link ParticipantAgent} by evaluating the access control and
 * usage policies associated with a set of assets as defined by {@link ContractDefinition}s. On the distinction between
 * access control and usage policy, see {@link ContractDefinition}.
 */
public class ContentBasedContractDefinitionResolverImpl implements ContractDefinitionResolver {
    private final PolicyEngine policyEngine;
    private final PolicyDefinitionStore policyStore;
    private final ContractDefinitionStore definitionStore;

    public ContentBasedContractDefinitionResolverImpl(ContractDefinitionStore contractDefinitionStore, PolicyEngine policyEngine, PolicyDefinitionStore policyStore) {
        definitionStore = contractDefinitionStore;
        this.policyEngine = policyEngine;
        this.policyStore = policyStore;
    }

    @Override
    public ResolvedContractDefinitions resolveFor(ParticipantAgent agent) {
        var policies = new HashMap<String, Policy>();
        var definitions = definitionStore.findAll(QuerySpec.max())
                .filter(definition -> {
                    var accessResult = Optional.of(definition.getAccessPolicyId())
                            .map(policyId -> policies.computeIfAbsent(policyId,
                                    key -> Optional.ofNullable(policyStore.findById(key))
                                            .map(PolicyDefinition::getPolicy)
                                            .orElse(null))
                            )
                            .map(policy -> policyEngine.evaluate(policy, new CatalogPolicyContext(agent)))
                            .orElse(Result.failure(String.format("Policy %s not found", definition.getAccessPolicyId())));

                    return accessResult.succeeded();
                })
                .toList();

        return new ResolvedContractDefinitions(definitions, policies);
    }

}
