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


import jakarta.json.JsonObject;
import jakarta.json.JsonStructure;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.heleade.common.FederatedCatalogCommon.addNodeToDirectory;
import static org.eclipse.edc.heleade.common.FederatedCatalogCommon.getEmbeddedFc;
import static org.eclipse.edc.heleade.common.NegotiationCommon.getContractNegotiationState;
import static org.eclipse.edc.heleade.common.PolicyCommon.checkPolicyById;
import static org.eclipse.edc.heleade.common.PolicyCommon.createAssetWithId;
import static org.eclipse.edc.heleade.common.PolicyCommon.createContractDefinitionWithParams;
import static org.eclipse.edc.heleade.common.PolicyCommon.createPolicyComplex;
import static org.eclipse.edc.heleade.common.PolicyCommon.createPolicyWithParams;
import static org.eclipse.edc.heleade.common.PolicyCommon.createSimpleDynamicPolicy;
import static org.eclipse.edc.heleade.common.PolicyCommon.fetchDatasetFromCatalogWithId;
import static org.eclipse.edc.heleade.common.PolicyCommon.generateKeys;
import static org.eclipse.edc.heleade.common.PolicyCommon.negotiateContractComplex;
import static org.eclipse.edc.heleade.common.PolicyCommon.negotiateContractWithParams;
import static org.eclipse.edc.heleade.common.PolicyCommon.verifyIdentity;
import static org.eclipse.edc.heleade.common.PrerequisitesCommon.getConsumer;
import static org.eclipse.edc.heleade.common.PrerequisitesCommon.getProvider;
import static org.eclipse.edc.heleade.util.TransferUtil.POLL_INTERVAL;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

@EndToEndTest
public class Policy01BasicTest {
    private static final Duration TIMEOUT = Duration.ofSeconds(190);
    private static final String CONSUMER_URL_API = "http://localhost:39191/api";
    private static final String PROVIDER_URL_API = "http://localhost:49191/api";
    private static final String RESOURCES_FOLDER = "system-tests/src/test/resources/policy";
    private static final String PROVIDER_NODE_DIRECTORY_PATH = RESOURCES_FOLDER + "/provider-directory-local.json";
    private static final String CONSUMER_NODE_DIRECTORY_PATH = RESOURCES_FOLDER + "/consumer-directory-local.json";
    private static final String CONSUMER_KEY_PATH = "src/test/resources/keys/consumer";
    private static final String PROVIDER_KEY_PATH = "src/test/resources/keys/provider";
    private static final String CONSUMER_NODE_RELATIVE_PATH = "src/test/resources/policy/consumer-directory.json";
    private static final String PROVIDER_NODE_RELATIVE_PATH = "src/test/resources/policy/provider-directory.json";
    private static final String EU = "eu";
    private static final String US = "us";
    private static final String ENTITY_TYPE_PUBLIC = "public";
    private static final String N_EMPLOYEES_OK = "100";
    private static final String N_EMPLOYEES_KO = "7000";
    private static final String LEI_CODE_OK = "1000";
    private static final String LEI_CODE_KO = "70";
    private static final String INVALID_VALUE = "abc";
    private static final String ALLOWED_COUNTRIES = "esp,che,fra,prt";
    private static final String ALLOWED_COUNTRY = "esp";
    private static final String ALLOWED_AWARD = "Red Dot Design Award";
    private static final String ALLOWED_PARTICIPANT = "consumer";
    private static final String ALLOWED_PARTICIPANTS = "consumer,provider-ebird,consumer-base";
    private static final String LEFT_OPERAND_LOCATION = "location";
    private static final String LEFT_OPERAND_ENTITY_TYPE = "entity_type";
    private static final String LEFT_OPERAND_PARTICIPANT_ID = "participant_id";
    private static final String LEFT_OPERAND_COUNTRY = "country";
    private static final String LEFT_OPERAND_AWARD = "https://schema.org/award";
    private static final String LEFT_OPERAND_POLICY_EVALUATION_TIME = "policy_evaluation_time";
    private static final String POLICY_OPEN_ID = "always-true";
    private static final String POLICY_LOCATION_EU_ID = "policy-location-eu";
    private static final String POLICY_LOCATION_US_ID = "policy-location-us";
    private static final String POLICY_ENTITY_TYPE_PUBLIC_ID = "policy-entity-type-public";
    private static final String POLICY_COUNTRY_EQ_ID = "policy-country-connector-eq";
    private static final String POLICY_COUNTRY_IN_ID = "policy-country-connector-in";
    private static final String POLICY_TIME_CHECKER_LT_ID = "policy-time-checker-lt";
    private static final String POLICY_TIME_CHECKER_GT_ID = "policy-time-checker-gt";
    private static final String POLICY_TIME_CHECKER_NEQ_ID = "policy-time-checker-neq";
    private static final String POLICY_TIME_CHECKER_INVALID_ID = "policy-time-checker-invalid";
    private static final String POLICY_PARTICIPANT_ID_EQ_ID = "policy-participant-id-eq";
    private static final String POLICY_PARTICIPANT_ID_IN_ID = "policy-participant-id-in";
    private static final String POLICY_COMPLEX_AND_ID = "policy-complex-and";
    private static final String POLICY_COMPLEX_OR_ID = "policy-complex-or";
    private static final String POLICY_SIMPLE_DYNAMIC = "policy-simple-dynamic";
    private static final String POLICY_COMPLEX_AND_ID_TERMINATED = "policy-complex-and-ko";
    private static final String POLICY_COMPLEX_ID_INVALID = "policy-complex-invalid";
    private static final String OPERATOR_IS_PART_OF = "odrl:isPartOf";
    private static final String OPERATOR_EQUAL = "odrl:eq";
    private static final String OPERATOR_NOT_EQUAL = "odrl:neq";
    private static final String OPERATOR_LESS_OR_EQUAL = "odrl:leq";
    private static final String OPERATOR_GREATER_OR_EQUAL = "odrl:geq";
    private static final String OPERATOR_GREATER = "odrl:gt";
    private static final String OPERATOR_LT = "odrl:lt";
    private static final String AND_OPERAND = "and";
    private static final String OR_OPERAND = "or";
    private static final String PROVIDER_CONFIG_PROPERTIES_FILE_PATH = "system-tests/src/test/resources/provider-test-configuration.properties";
    private static final String CONSUMER_MODULE_PATH = ":consumers:consumer-base";


    static {
        try {
            generateKeys(CONSUMER_KEY_PATH, CONSUMER_NODE_RELATIVE_PATH);
            generateKeys(PROVIDER_KEY_PATH, PROVIDER_NODE_RELATIVE_PATH);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @RegisterExtension
    static final RuntimeExtension FC_RUNTIME = getEmbeddedFc(":federated-catalog");

    @RegisterExtension
    static final RuntimeExtension PROVIDER_RUNTIME = getProvider(":providers:provider-ebird", PROVIDER_CONFIG_PROPERTIES_FILE_PATH);

    @RegisterExtension
    static final RuntimeExtension CONSUMER_RUNTIME = getConsumer(CONSUMER_MODULE_PATH);


    @BeforeAll
    static void beforeAll() {
        addNodeToDirectory(CONSUMER_NODE_DIRECTORY_PATH);
    }

    @Test
    void testPolicyAlwaysTrueExist() {
        boolean policyAlwaysTrueExist = checkPolicyById("always-true");
        assertThat(policyAlwaysTrueExist).isTrue();
    }

    /*  @Test
    void testPolicyEntityTypePrivateAssetIsNotInCatalog() {
        String id = UUID.randomUUID().toString();
        createAssetWithId(id);
        createPolicyWithParams(POLICY_ENTITY_TYPE_PRIVATE_ID, LEFT_OPERAND_ENTITY_TYPE, ENTITY_TYPE_PRIVATE, OPERATOR_EQUAL);
        createContractDefinitionWithParams(id, POLICY_ENTITY_TYPE_PRIVATE_ID, POLICY_OPEN_ID, id);
        ArrayList<LinkedHashMap> catalogDatasets = fetchCatalogDatasets(CATALOG_REQUEST_FILE_PATH);
        boolean catalogContainsAsset = catalogContainsAssetId(id, catalogDatasets);
        assertThat(catalogContainsAsset).isFalse();
    }*/


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
        var contractNegotiationId = negotiateContractWithParams(id, catalogDatasetId, LEFT_OPERAND_LOCATION, EU, OPERATOR_EQUAL, true);
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
        var contractNegotiationId = negotiateContractWithParams(id, catalogDatasetId, LEFT_OPERAND_LOCATION, US, OPERATOR_EQUAL, true);
        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL)
                .until(() -> getContractNegotiationState(contractNegotiationId), s -> s.equals("TERMINATED"));
    }

    @Test
    void testCountryInFinalized() {
        String id = UUID.randomUUID().toString();
        createAssetWithId(id);
        createPolicyWithParams(POLICY_COUNTRY_IN_ID, LEFT_OPERAND_COUNTRY, ALLOWED_COUNTRIES, OPERATOR_IS_PART_OF);
        createContractDefinitionWithParams(id, POLICY_OPEN_ID, POLICY_COUNTRY_IN_ID, id);
        var catalogDatasetId = fetchDatasetFromCatalogWithId(id);
        var contractNegotiationId = negotiateContractWithParams(id, catalogDatasetId, LEFT_OPERAND_COUNTRY, ALLOWED_COUNTRIES, OPERATOR_IS_PART_OF, true);
        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL)
                .until(() -> getContractNegotiationState(contractNegotiationId), s -> s.equals("FINALIZED"));
    }

    @Test
    void testCountryEqFinalized() {
        String id = UUID.randomUUID().toString();
        createAssetWithId(id);
        createPolicyWithParams(POLICY_COUNTRY_EQ_ID, LEFT_OPERAND_COUNTRY, ALLOWED_COUNTRY, OPERATOR_EQUAL);
        createContractDefinitionWithParams(id, POLICY_OPEN_ID, POLICY_COUNTRY_EQ_ID, id);
        var catalogDatasetId = fetchDatasetFromCatalogWithId(id);
        var contractNegotiationId = negotiateContractWithParams(id, catalogDatasetId, LEFT_OPERAND_COUNTRY, ALLOWED_COUNTRY, OPERATOR_EQUAL, true);
        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL)
                .until(() -> getContractNegotiationState(contractNegotiationId), s -> s.equals("FINALIZED"));
    }


    @Test
    void testParticipantIdEqFinalized() {
        String id = UUID.randomUUID().toString();
        createAssetWithId(id);
        createPolicyWithParams(POLICY_PARTICIPANT_ID_EQ_ID, LEFT_OPERAND_PARTICIPANT_ID, ALLOWED_PARTICIPANT, OPERATOR_EQUAL);
        createContractDefinitionWithParams(id, POLICY_OPEN_ID, POLICY_PARTICIPANT_ID_EQ_ID, id);
        var catalogDatasetId = fetchDatasetFromCatalogWithId(id);
        var contractNegotiationId = negotiateContractWithParams(id, catalogDatasetId, LEFT_OPERAND_PARTICIPANT_ID, ALLOWED_PARTICIPANT, OPERATOR_EQUAL, true);
        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL)
                .until(() -> getContractNegotiationState(contractNegotiationId), s -> s.equals("FINALIZED"));
    }


    @Test
    void testParticipantIdInFinalized() {
        String id = UUID.randomUUID().toString();
        createAssetWithId(id);
        createPolicyWithParams(POLICY_PARTICIPANT_ID_IN_ID, LEFT_OPERAND_PARTICIPANT_ID, ALLOWED_PARTICIPANTS, OPERATOR_IS_PART_OF);
        createContractDefinitionWithParams(id, POLICY_OPEN_ID, POLICY_PARTICIPANT_ID_IN_ID, id);
        var catalogDatasetId = fetchDatasetFromCatalogWithId(id);
        var contractNegotiationId = negotiateContractWithParams(id, catalogDatasetId, LEFT_OPERAND_PARTICIPANT_ID, ALLOWED_PARTICIPANTS, OPERATOR_IS_PART_OF, true);
        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL)
                .until(() -> getContractNegotiationState(contractNegotiationId), s -> s.equals("FINALIZED"));
    }


    @Test
    void testTimePolicyCheckLtShouldBeFinalized() {
        String id = UUID.randomUUID().toString();
        createAssetWithId(id);
        String rightOperand = OffsetDateTime.now().plusDays(1).toString();
        createPolicyWithParams(POLICY_TIME_CHECKER_LT_ID, LEFT_OPERAND_POLICY_EVALUATION_TIME, rightOperand, OPERATOR_LT);
        createContractDefinitionWithParams(id, POLICY_OPEN_ID, POLICY_TIME_CHECKER_LT_ID, id);
        var catalogDatasetId = fetchDatasetFromCatalogWithId(id);
        var contractNegotiationId = negotiateContractWithParams(id, catalogDatasetId, LEFT_OPERAND_POLICY_EVALUATION_TIME, rightOperand, OPERATOR_LT, true);
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
        var contractNegotiationId = negotiateContractWithParams(id, catalogDatasetId, LEFT_OPERAND_POLICY_EVALUATION_TIME, rightOperand, OPERATOR_GREATER, true);
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
        var contractNegotiationId = negotiateContractWithParams(id, catalogDatasetId, LEFT_OPERAND_POLICY_EVALUATION_TIME, rightOperand, OPERATOR_GREATER, true);
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
        var contractNegotiationId = negotiateContractWithParams(id, catalogDatasetId, LEFT_OPERAND_POLICY_EVALUATION_TIME, rightOperand, OPERATOR_NOT_EQUAL, true);
        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL)
                .until(() -> getContractNegotiationState(contractNegotiationId), s -> s.equals("FINALIZED"));
    }


    @Test
    void testComplexPolicyAndShouldBeFinalized() {
        String id = UUID.randomUUID().toString();
        String rightOperand = OffsetDateTime.now().minusDays(1).toString();
        createAssetWithId(id);
        createPolicyComplex(POLICY_COMPLEX_AND_ID, AND_OPERAND, rightOperand, LEI_CODE_OK, N_EMPLOYEES_OK);
        createContractDefinitionWithParams(id, POLICY_OPEN_ID, POLICY_COMPLEX_AND_ID, id);
        var catalogDatasetId = fetchDatasetFromCatalogWithId(id);
        var contractNegotiationId = negotiateContractComplex(id, catalogDatasetId, AND_OPERAND, rightOperand, LEI_CODE_OK, N_EMPLOYEES_OK);
        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL)
                .until(() -> getContractNegotiationState(contractNegotiationId), s -> s.equals("FINALIZED"));


    }

    @Test
    void testComplexPolicyInvalidShouldBeTerminated() {
        String id = UUID.randomUUID().toString();
        String rightOperand = OffsetDateTime.now().minusDays(1).toString();
        createAssetWithId(id);
        createPolicyComplex(POLICY_COMPLEX_ID_INVALID, AND_OPERAND, rightOperand, INVALID_VALUE, INVALID_VALUE);
        createContractDefinitionWithParams(id, POLICY_OPEN_ID, POLICY_COMPLEX_ID_INVALID, id);
        var catalogDatasetId = fetchDatasetFromCatalogWithId(id);
        var contractNegotiationId = negotiateContractComplex(id, catalogDatasetId, AND_OPERAND, rightOperand, INVALID_VALUE, INVALID_VALUE);
        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL)
                .until(() -> getContractNegotiationState(contractNegotiationId), s -> s.equals("TERMINATED"));

    }

    @Test
    void testComplexPolicyAndShouldBeTerminated() {
        String id = UUID.randomUUID().toString();
        String rightOperand = OffsetDateTime.now().minusDays(1).toString();
        createAssetWithId(id);
        createPolicyComplex(POLICY_COMPLEX_AND_ID_TERMINATED, AND_OPERAND, rightOperand, LEI_CODE_KO, N_EMPLOYEES_KO);
        createContractDefinitionWithParams(id, POLICY_OPEN_ID, POLICY_COMPLEX_AND_ID_TERMINATED, id);
        var catalogDatasetId = fetchDatasetFromCatalogWithId(id);
        var contractNegotiationId = negotiateContractComplex(id, catalogDatasetId, AND_OPERAND, rightOperand, LEI_CODE_KO, N_EMPLOYEES_KO);
        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL)
                .until(() -> getContractNegotiationState(contractNegotiationId), s -> s.equals("TERMINATED"));


    }


    @Test
    void testComplexPolicyOrShouldBeFinalized() {
        String id = UUID.randomUUID().toString();
        String rightOperand = OffsetDateTime.now().minusDays(1).toString();
        createAssetWithId(id);
        createPolicyComplex(POLICY_COMPLEX_OR_ID, OR_OPERAND, rightOperand, LEI_CODE_KO, N_EMPLOYEES_KO);
        createContractDefinitionWithParams(id, POLICY_OPEN_ID, POLICY_COMPLEX_OR_ID, id);
        var catalogDatasetId = fetchDatasetFromCatalogWithId(id);
        var contractNegotiationId = negotiateContractComplex(id, catalogDatasetId, OR_OPERAND, rightOperand, LEI_CODE_KO, N_EMPLOYEES_KO);
        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL)
                .until(() -> getContractNegotiationState(contractNegotiationId), s -> s.equals("FINALIZED"));

    }

    @Test
    void testSimpleDynamicPolicyAnotherNamespace() {
        String id = UUID.randomUUID().toString();
        createAssetWithId(id);
        createSimpleDynamicPolicy(POLICY_SIMPLE_DYNAMIC);
        createContractDefinitionWithParams(id, POLICY_OPEN_ID, POLICY_SIMPLE_DYNAMIC, id);
        var catalogDatasetId = fetchDatasetFromCatalogWithId(id);
        var contractNegotiationId = negotiateContractWithParams(id, catalogDatasetId, LEFT_OPERAND_AWARD, ALLOWED_AWARD, OPERATOR_EQUAL, false);
        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL)
                .until(() -> getContractNegotiationState(contractNegotiationId), s -> s.equals("FINALIZED"));

    }

    @Test
    void autoVerifySuccess() {
        JsonStructure response =  verifyIdentity(CONSUMER_URL_API);
        JsonObject json = response.asJsonObject();
        Boolean signatureResult = json.getBoolean(EDC_NAMESPACE + "success");
        assertThat(signatureResult).isEqualTo(true);

    }


    @Test
    void autoVerifyFail() {
        JsonStructure response =  verifyIdentity(PROVIDER_URL_API);
        JsonObject json = response.asJsonObject();
        Boolean signatureResult = json.getBoolean(EDC_NAMESPACE + "success");
        assertThat(signatureResult).isEqualTo(false);
    }


}
