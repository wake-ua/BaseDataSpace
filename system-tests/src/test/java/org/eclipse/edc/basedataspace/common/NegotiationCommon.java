/*
 *  Copyright (c) 2023 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Initial implementation
 *       Fraunhofer Institute for Software and Systems Engineering - use current ids instead of placeholder
 *
 */

package org.eclipse.edc.basedataspace.common;


import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.basedataspace.common.FileTransferCommon.getFileContentFromRelativePath;
import static org.eclipse.edc.basedataspace.util.TransferUtil.POLL_INTERVAL;
import static org.eclipse.edc.basedataspace.util.TransferUtil.TIMEOUT;
import static org.eclipse.edc.basedataspace.util.TransferUtil.get;
import static org.eclipse.edc.basedataspace.util.TransferUtil.post;
import static org.eclipse.edc.basedataspace.util.TransferUtil.postObject;

public class NegotiationCommon {

    private static final String CREATE_ASSET_FILE_PATH = "system-tests/src/test/resources/transfer/create-asset.json";
    private static final String V3_ASSETS_PATH = "/v3/assets";
    private static final String CREATE_POLICY_FILE_PATH = "system-tests/src/test/resources/transfer/create-policy.json";
    private static final String V2_POLICY_DEFINITIONS_PATH = "/v3/policydefinitions";
    private static final String CREATE_CONTRACT_DEFINITION_FILE_PATH = "system-tests/src/test/resources/transfer/create-contract-definition.json";
    private static final String V2_CONTRACT_DEFINITIONS_PATH = "/v3/contractdefinitions";
    private static final String V2_CATALOG_DATASET_REQUEST_PATH = "/v3/catalog/dataset/request";
    private static final String FETCH_DATASET_FROM_CATALOG_FILE_PATH = "system-tests/src/test/resources/transfer/get-dataset.json";
    private static final String V2_CATALOG_REQUEST_PATH = "/v3/catalog/request";
    private static final String CATALOG_DATASET_ID = "\"odrl:hasPolicy\".'@id'";
    private static final String CATALOG_DATASET_LIST = "\"dcat:dataset\"";
    private static final String NEGOTIATE_CONTRACT_FILE_PATH = "system-tests/src/test/resources/transfer/negotiate-contract.json";
    private static final String V2_CONTRACT_NEGOTIATIONS_PATH = "/v3/contractnegotiations/";
    private static final String CONTRACT_NEGOTIATION_ID = "@id";
    private static final String CONTRACT_AGREEMENT_ID = "contractAgreementId";
    private static final String CONTRACT_OFFER_ID_KEY = "{{contract-offer-id}}";

    public static void createAsset() {
        createAsset(CREATE_ASSET_FILE_PATH);
    }

    public static void createAsset(String assetFilePath) {
        post(PrerequisitesCommon.PROVIDER_MANAGEMENT_URL + V3_ASSETS_PATH, getFileContentFromRelativePath(assetFilePath));
    }

    public static void createPolicy() {
        createPolicy(CREATE_POLICY_FILE_PATH);
    }

    public static void createPolicy(String createPolicyFilePath) {
        post(PrerequisitesCommon.PROVIDER_MANAGEMENT_URL + V2_POLICY_DEFINITIONS_PATH, getFileContentFromRelativePath(createPolicyFilePath));
    }
    public static void createContractDefinition() {
        createContractDefinition(CREATE_CONTRACT_DEFINITION_FILE_PATH);
    }

    public static void createContractDefinition(String createContractDefinitionFilePath) {
        post(PrerequisitesCommon.PROVIDER_MANAGEMENT_URL + V2_CONTRACT_DEFINITIONS_PATH, getFileContentFromRelativePath(createContractDefinitionFilePath));
    }

    public static String fetchDatasetFromCatalog(String fetchDatasetFromCatalogFilePath) {
        var catalogDatasetId = post(
                PrerequisitesCommon.CONSUMER_MANAGEMENT_URL + V2_CATALOG_DATASET_REQUEST_PATH,
                getFileContentFromRelativePath(fetchDatasetFromCatalogFilePath),
                CATALOG_DATASET_ID
        );
        assertThat(catalogDatasetId).isNotEmpty();
        return catalogDatasetId;
    }

    public static Object fetchCatalogDatasets(String catalogRequestFilePath) {
        var catalogDatasets = postObject(
                PrerequisitesCommon.CONSUMER_MANAGEMENT_URL + V2_CATALOG_REQUEST_PATH,
                getFileContentFromRelativePath(catalogRequestFilePath), CATALOG_DATASET_LIST);
        assertThat(catalogDatasets).isNotNull();
        return catalogDatasets;
    }

    public static String negotiateContract(String negotiateContractFilePath, String catalogDatasetId) {
        var requestBody = getFileContentFromRelativePath(negotiateContractFilePath)
                .replace(CONTRACT_OFFER_ID_KEY, catalogDatasetId);
        var contractNegotiationId = post(
                PrerequisitesCommon.CONSUMER_MANAGEMENT_URL + V2_CONTRACT_NEGOTIATIONS_PATH,
                requestBody,
                CONTRACT_NEGOTIATION_ID
        );
        assertThat(contractNegotiationId).isNotEmpty();
        return contractNegotiationId;
    }

    public static String getContractAgreementId(String contractNegotiationId) {
        var url = PrerequisitesCommon.CONSUMER_MANAGEMENT_URL + V2_CONTRACT_NEGOTIATIONS_PATH + contractNegotiationId;
        return await()
                .atMost(TIMEOUT)
                .pollInterval(POLL_INTERVAL)
                .until(() -> get(url, CONTRACT_AGREEMENT_ID), Objects::nonNull);
    }

    public static String getContractNegotiationState(String contractNegotiationId) {
        var url = PrerequisitesCommon.CONSUMER_MANAGEMENT_URL + V2_CONTRACT_NEGOTIATIONS_PATH + contractNegotiationId;
        return get(url, "state");
    }

    public static String runNegotiation() {
        createAsset();
        createPolicy();
        createContractDefinition();
        var catalogDatasetId = fetchDatasetFromCatalog(FETCH_DATASET_FROM_CATALOG_FILE_PATH);
        var contractNegotiationId = negotiateContract(NEGOTIATE_CONTRACT_FILE_PATH, catalogDatasetId);
        return getContractAgreementId(contractNegotiationId);
    }
}
