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


import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.ContractDefinitionResolver;
import org.eclipse.edc.connector.controlplane.catalog.spi.DataService;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.connector.controlplane.catalog.spi.DatasetResolver;
import org.eclipse.edc.connector.controlplane.catalog.spi.DistributionResolver;
import org.eclipse.edc.connector.controlplane.contract.spi.ContractOfferId;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.dataaddress.httpdata.spi.HttpDataAddressSchema;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.PolicyType;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.query.QuerySpec;
import org.jetbrains.annotations.NotNull;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.lang.Integer.MAX_VALUE;

public class ContentBasedDatasetResolverImpl implements DatasetResolver {

    private final ContractDefinitionResolver contractDefinitionResolver;
    private final AssetIndex assetIndex;
    private final PolicyDefinitionStore policyDefinitionStore;
    private final DistributionResolver distributionResolver;
    private final CriterionOperatorRegistry criterionOperatorRegistry;

    public ContentBasedDatasetResolverImpl(ContractDefinitionResolver contractDefinitionResolver, AssetIndex assetIndex,
                               PolicyDefinitionStore policyDefinitionStore, DistributionResolver distributionResolver,
                               CriterionOperatorRegistry criterionOperatorRegistry) {
        this.contractDefinitionResolver = contractDefinitionResolver;
        this.assetIndex = assetIndex;
        this.policyDefinitionStore = policyDefinitionStore;
        this.distributionResolver = distributionResolver;
        this.criterionOperatorRegistry = criterionOperatorRegistry;
    }

    @Override
    @NotNull
    public Stream<Dataset> query(ParticipantAgent agent, QuerySpec querySpec) {
        var resolved = contractDefinitionResolver.resolveFor(agent);
        var contractDefinitions = resolved.contractDefinitions();
        if (contractDefinitions.isEmpty()) {
            return Stream.empty();
        }

        var assetsQuery = QuerySpec.Builder.newInstance().offset(0).limit(MAX_VALUE).filter(querySpec.getFilterExpression()).build();
        return assetIndex.queryAssets(assetsQuery)
                .map(asset -> toDataset(contractDefinitions, asset, resolved.policies()))
                .filter(Dataset::hasOffers)
                .skip(querySpec.getOffset())
                .limit(querySpec.getLimit());
    }

    @Override
    public Dataset getById(ParticipantAgent agent, String id) {
        var resolved = contractDefinitionResolver.resolveFor(agent);
        var contractDefinitions = resolved.contractDefinitions();
        if (contractDefinitions.isEmpty()) {
            return null;
        }

        return Optional.of(id)
                .map(assetIndex::findById)
                .map(asset -> toDataset(contractDefinitions, asset, resolved.policies()))
                .filter(Dataset::hasOffers)
                .orElse(null);
    }

    private Dataset.Builder<?, ?> buildDataset(Asset asset) {
        if (!asset.isCatalog()) {
            return Dataset.Builder.newInstance();
        }

        return Catalog.Builder.newInstance()
                .dataService(DataService.Builder.newInstance()
                        .id(Base64.getUrlEncoder().encodeToString(asset.getId().getBytes()))
                        .endpointDescription(asset.getDescription())
                        .endpointUrl(asset.getDataAddress().getStringProperty(HttpDataAddressSchema.BASE_URL, null))
                        .build());
    }

    private Dataset toDataset(List<ContractDefinition> contractDefinitions, Asset asset, Map<String, Policy> policies) {

        var distributions = distributionResolver.getDistributions(asset);
        var datasetBuilder = buildDataset(asset)
                .id(asset.getId())
                .distributions(distributions)
                .properties(asset.getProperties());

        contractDefinitions.stream()
                .filter(definition -> definition.getAssetsSelector().stream()
                        .map(criterionOperatorRegistry::toPredicate)
                        .reduce(x -> true, Predicate::and)
                        .test(asset)
                )
                .forEach(contractDefinition -> {
                    var policy = policies.computeIfAbsent(contractDefinition.getContractPolicyId(), policyId ->
                            Optional.ofNullable(policyDefinitionStore.findById(policyId))
                                    .map(PolicyDefinition::getPolicy)
                                    .orElse(null)
                    );

                    if (policy != null) {
                        var contractId = ContractOfferId.create(contractDefinition.getId(), asset.getId());
                        var offerPolicy = policy.toBuilder().type(PolicyType.OFFER).build();
                        datasetBuilder.offer(contractId.toString(), offerPolicy);
                    }
                });

        return datasetBuilder.build();
    }

}
