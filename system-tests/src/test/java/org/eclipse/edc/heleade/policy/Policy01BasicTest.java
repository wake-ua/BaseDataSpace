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

package org.eclipse.edc.heleade.policy;


import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.heleade.common.FederatedCatalogCommon.addNodeToDirectory;
import static org.eclipse.edc.heleade.common.FederatedCatalogCommon.getEmbeddedFc;
import static org.eclipse.edc.heleade.common.NegotiationCommon.fetchCatalogDatasets;
import static org.eclipse.edc.heleade.common.NegotiationCommon.getContractNegotiationState;
import static org.eclipse.edc.heleade.common.PolicyCommon.catalogContainsAssetId;
import static org.eclipse.edc.heleade.common.PolicyCommon.checkPolicyById;
import static org.eclipse.edc.heleade.common.PolicyCommon.createAssetWithId;
import static org.eclipse.edc.heleade.common.PolicyCommon.createContractDefinitionWithParams;
import static org.eclipse.edc.heleade.common.PolicyCommon.createPolicyWithParams;
import static org.eclipse.edc.heleade.common.PolicyCommon.fetchDatasetFromCatalogWithId;
import static org.eclipse.edc.heleade.common.PolicyCommon.negotiateContractWithParams;
import static org.eclipse.edc.heleade.common.PrerequisitesCommon.getConsumer;
import static org.eclipse.edc.heleade.common.PrerequisitesCommon.getProvider;
import static org.eclipse.edc.heleade.util.TransferUtil.POLL_INTERVAL;

@EndToEndTest
public class Policy01BasicTest {
    private static final Duration TIMEOUT = Duration.ofSeconds(190);
    private static final String RESOURCES_FOLDER = "system-tests/src/test/resources/policy";
    private static final String PROVIDER_NODE_DIRECTORY_PATH = RESOURCES_FOLDER + "/provider-participant-directory.json";
    private static final String CONSUMER_NODE_DIRECTORY_PATH = RESOURCES_FOLDER + "/consumer-participant-directory.json";
    private static final String CATALOG_REQUEST_FILE_PATH = RESOURCES_FOLDER + "/catalog-request.json";
    private static final String EU = "eu";
    private static final String US = "us";
    private static final String ENTITY_TYPE_PUBLIC = "public";
    private static final String ENTITY_TYPE_PRIVATE = "private";
    private static final String IPS_OK = "219.208.53.217,219.208.53.219";
    private static final String IPS_KO = "209.202.53.217,229.208.53.666";
    private static final String LEFT_OPERAND_LOCATION = "location";
    private static final String LEFT_OPERAND_ENTITY_TYPE = "entity_type";
    private static final String LEFT_OPERAND_IP_CONNECTOR = "ip_connector";
    private static final String LEFT_OPERAND_POLICY_EVALUATION_TIME  = "policy_evaluation_time";
    private static final String POLICY_OPEN_ID = "always-true";
    private static final String POLICY_LOCATION_EU_ID = "policy-location-eu";
    private static final String POLICY_LOCATION_US_ID = "policy-location-us";
    private static final String POLICY_ENTITY_TYPE_PUBLIC_ID = "policy-entity-type-public";
    private static final String POLICY_ENTITY_TYPE_PRIVATE_ID = "policy-entity-type-private";
    private static final String POLICY_IP_OK_CONNECTOR_ID = "policy-ip-connector-ok";
    private static final String POLICY_IP_KO_CONNECTOR_ID = "policy-ip-connector-ko";
    private static final String POLICY_TIME_CHECKER_LT_ID = "policy-time-checker-lt";
    private static final String POLICY_TIME_CHECKER_GT_ID = "policy-time-checker-gt";
    private static final String POLICY_TIME_CHECKER_EQ_ID = "policy-time-checker-eq";
    private static final String POLICY_TIME_CHECKER_NEQ_ID = "policy-time-checker-neq";
    private static final String POLICY_TIME_CHECKER_LEQ_ID = "policy-time-checker-leq";
    private static final String POLICY_TIME_CHECKER_GEQ_ID = "policy-time-checker-geq";
    private static final String POLICY_TIME_CHECKER_INVALID_ID = "policy-time-checker-invalid";
    private static final String OPERATOR_IS_PART_OF = "odrl:isPartOf";
    private static final String OPERATOR_EQUAL = "odrl:eq";
    private static final String OPERATOR_NOT_EQUAL = "odrl:neq";
    private static final String OPERATOR_LESS_OR_EQUAL = "odrl:leq";
    private static final String OPERATOR_GREATER_OR_EQUAL = "odrl:geq";
    private static final String OPERATOR_GREATER = "odrl:gt";
    private static final String OPERATOR_LT = "odrl:lt";
    private static final String PROVIDER_CONFIG_PROPERTIES_FILE_PATH = "system-tests/src/test/resources/provider-test-configuration.properties";
    private static final String CONSUMER_MODULE_PATH = ":consumers:consumer-base";

    @RegisterExtension
    static final RuntimeExtension FC_RUNTIME = getEmbeddedFc(":federated-catalog");

    @RegisterExtension
    static final RuntimeExtension PROVIDER_RUNTIME = getProvider(":providers:provider-ebird", PROVIDER_CONFIG_PROPERTIES_FILE_PATH);

    @RegisterExtension
    static final RuntimeExtension CONSUMER_RUNTIME = getConsumer(CONSUMER_MODULE_PATH);


    @BeforeAll
    static void beforeAll() {
        addNodeToDirectory(PROVIDER_NODE_DIRECTORY_PATH);
        addNodeToDirectory(CONSUMER_NODE_DIRECTORY_PATH);
    }

    @Test
    void testPolicyAlwaysTrueExist() {
        boolean policyAlwaysTrueExist = checkPolicyById("always-true");
        assertThat(policyAlwaysTrueExist).isTrue();
    }

    @Test
    void testPolicyEntityTypePrivateAssetIsNotInCatalog() {
        String id = UUID.randomUUID().toString();
        createAssetWithId(id);
        createPolicyWithParams(POLICY_ENTITY_TYPE_PRIVATE_ID, LEFT_OPERAND_ENTITY_TYPE, ENTITY_TYPE_PRIVATE, OPERATOR_EQUAL);
        createContractDefinitionWithParams(id, POLICY_ENTITY_TYPE_PRIVATE_ID, POLICY_OPEN_ID, id);
        ArrayList<LinkedHashMap> catalogDatasets = fetchCatalogDatasets(CATALOG_REQUEST_FILE_PATH);
        boolean catalogContainsAsset = catalogContainsAssetId(id, catalogDatasets);
        assertThat(catalogContainsAsset).isFalse();
    }


    @Test
    void testPolicyEntityTypePublicAssetIsInCatalog() {
        String id = UUID.randomUUID().toString();
        createAssetWithId(id);
        createPolicyWithParams(POLICY_ENTITY_TYPE_PUBLIC_ID, LEFT_OPERAND_ENTITY_TYPE, ENTITY_TYPE_PUBLIC, OPERATOR_EQUAL);
        createContractDefinitionWithParams(id, POLICY_ENTITY_TYPE_PUBLIC_ID, POLICY_OPEN_ID, id);
        var catalogDatasetId = fetchDatasetFromCatalogWithId(id);
        assertThat(catalogDatasetId).isNotEmpty();
    }

    @Test
    void testLocationEuFinalized() {
        String id = UUID.randomUUID().toString();
        createAssetWithId(id);
        createPolicyWithParams(POLICY_LOCATION_EU_ID, LEFT_OPERAND_LOCATION, EU, OPERATOR_EQUAL);
        createContractDefinitionWithParams(id, POLICY_OPEN_ID, POLICY_LOCATION_EU_ID, id);
        var catalogDatasetId = fetchDatasetFromCatalogWithId(id);
        var contractNegotiationId = negotiateContractWithParams(id, catalogDatasetId, LEFT_OPERAND_LOCATION, EU, OPERATOR_EQUAL);
        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL)
                .until(() -> getContractNegotiationState(contractNegotiationId), s -> s.equals("FINALIZED"));
    }

    @Test
    void testLocationUsTerminated() {
        String id = UUID.randomUUID().toString();
        createAssetWithId(id);
        createPolicyWithParams(POLICY_LOCATION_US_ID, LEFT_OPERAND_LOCATION, US, OPERATOR_EQUAL);
        createContractDefinitionWithParams(id, POLICY_OPEN_ID, POLICY_LOCATION_US_ID, id);
        var catalogDatasetId = fetchDatasetFromCatalogWithId(id);
        var contractNegotiationId = negotiateContractWithParams(id, catalogDatasetId, LEFT_OPERAND_LOCATION, US, OPERATOR_EQUAL);
        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL)
                .until(() -> getContractNegotiationState(contractNegotiationId), s -> s.equals("TERMINATED"));
    }

    @Test
    void testIpConnectorFinalized() {
        String id = UUID.randomUUID().toString();
        createAssetWithId(id);
        createPolicyWithParams(POLICY_IP_OK_CONNECTOR_ID, LEFT_OPERAND_IP_CONNECTOR, IPS_OK, OPERATOR_IS_PART_OF);
        createContractDefinitionWithParams(id, POLICY_OPEN_ID, POLICY_IP_OK_CONNECTOR_ID, id);
        var catalogDatasetId = fetchDatasetFromCatalogWithId(id);
        var contractNegotiationId = negotiateContractWithParams(id, catalogDatasetId, LEFT_OPERAND_IP_CONNECTOR, IPS_OK, OPERATOR_IS_PART_OF);
        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL)
                .until(() -> getContractNegotiationState(contractNegotiationId), s -> s.equals("FINALIZED"));
    }

    @Test
    void testIpConnectorTerminated() {
        String id = UUID.randomUUID().toString();
        createAssetWithId(id);
        createPolicyWithParams(POLICY_IP_KO_CONNECTOR_ID, LEFT_OPERAND_IP_CONNECTOR, IPS_KO, OPERATOR_IS_PART_OF);
        createContractDefinitionWithParams(id, POLICY_OPEN_ID, POLICY_IP_KO_CONNECTOR_ID, id);
        var catalogDatasetId = fetchDatasetFromCatalogWithId(id);
        var contractNegotiationId = negotiateContractWithParams(id, catalogDatasetId, LEFT_OPERAND_IP_CONNECTOR, IPS_KO, OPERATOR_IS_PART_OF);
        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL)
                .until(() -> getContractNegotiationState(contractNegotiationId), s -> s.equals("TERMINATED"));
    }

    @Test
    void testTimePolicyCheckLtShouldBeFinalized() {
        String id = UUID.randomUUID().toString();
        createAssetWithId(id);
        String rightOperand = OffsetDateTime.now().plusDays(1).toString();
        createPolicyWithParams(POLICY_TIME_CHECKER_LT_ID, LEFT_OPERAND_POLICY_EVALUATION_TIME, rightOperand, OPERATOR_LT);
        createContractDefinitionWithParams(id, POLICY_OPEN_ID, POLICY_TIME_CHECKER_LT_ID, id);
        var catalogDatasetId = fetchDatasetFromCatalogWithId(id);
        var contractNegotiationId = negotiateContractWithParams(id, catalogDatasetId, LEFT_OPERAND_POLICY_EVALUATION_TIME, rightOperand, OPERATOR_LT);
        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL)
                .until(() -> getContractNegotiationState(contractNegotiationId), s -> s.equals("FINALIZED"));
    }

    @Test
    void testTimePolicyCheckGtShouldBeFinalized() {
        String id = UUID.randomUUID().toString();
        createAssetWithId(id);
        String rightOperand = OffsetDateTime.now().minusDays(1).toString();
        createPolicyWithParams(POLICY_TIME_CHECKER_GT_ID, LEFT_OPERAND_POLICY_EVALUATION_TIME, rightOperand, OPERATOR_GREATER);
        createContractDefinitionWithParams(id, POLICY_OPEN_ID, POLICY_TIME_CHECKER_GT_ID, id);
        var catalogDatasetId = fetchDatasetFromCatalogWithId(id);
        var contractNegotiationId = negotiateContractWithParams(id, catalogDatasetId, LEFT_OPERAND_POLICY_EVALUATION_TIME, rightOperand, OPERATOR_GREATER);
        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL)
                .until(() -> getContractNegotiationState(contractNegotiationId), s -> s.equals("FINALIZED"));
    }

    @Test
    void testTimePolicyCheckInvalidDateShouldBeTerminated() {
        String id = UUID.randomUUID().toString();
        createAssetWithId(id);
        String rightOperand = "not-a-date";
        createPolicyWithParams(POLICY_TIME_CHECKER_INVALID_ID, LEFT_OPERAND_POLICY_EVALUATION_TIME, rightOperand, OPERATOR_GREATER);
        createContractDefinitionWithParams(id, POLICY_OPEN_ID, POLICY_TIME_CHECKER_INVALID_ID, id);
        var catalogDatasetId = fetchDatasetFromCatalogWithId(id);
        var contractNegotiationId = negotiateContractWithParams(id, catalogDatasetId, LEFT_OPERAND_POLICY_EVALUATION_TIME, rightOperand, OPERATOR_GREATER);
        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL)
                .until(() -> getContractNegotiationState(contractNegotiationId), s -> s.equals("TERMINATED"));
    }

    @Test
    void testTimePolicyCheckNeqShouldBeFinalized() {
        String id = UUID.randomUUID().toString();
        createAssetWithId(id);
        String rightOperand = OffsetDateTime.now().plusDays(10).toString();
        createPolicyWithParams(POLICY_TIME_CHECKER_NEQ_ID, LEFT_OPERAND_POLICY_EVALUATION_TIME, rightOperand, OPERATOR_NOT_EQUAL);
        createContractDefinitionWithParams(id, POLICY_OPEN_ID, POLICY_TIME_CHECKER_NEQ_ID, id);
        var catalogDatasetId = fetchDatasetFromCatalogWithId(id);
        var contractNegotiationId = negotiateContractWithParams(id, catalogDatasetId, LEFT_OPERAND_POLICY_EVALUATION_TIME, rightOperand, OPERATOR_NOT_EQUAL);
        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL)
                .until(() -> getContractNegotiationState(contractNegotiationId), s -> s.equals("FINALIZED"));
    }
}
