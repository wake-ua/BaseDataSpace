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

package org.eclipse.edc.heleade.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.heleade.common.FileTransferCommon.getFileContentFromRelativePath;
import static org.eclipse.edc.heleade.util.TransferUtil.post;

public class PolicyCommon {
    private static final String V3_ASSETS_PATH = "/v3/assets";
    private static final String V2_POLICY_DEFINITIONS_PATH = "/v3/policydefinitions";
    private static final String V2_CATALOG_DATASET_REQUEST_PATH = "/v3/catalog/dataset/request";
    private static final String V2_CONTRACT_DEFINITIONS_PATH = "/v3/contractdefinitions";
    private static final String V2_CONTRACT_NEGOTIATIONS_PATH = "/v3/contractnegotiations/";
    private static final String RESOURCES_FOLDER = "system-tests/src/test/resources/policy";
    private static final String CREATE_ASSET_FILE_PATH = RESOURCES_FOLDER + "/create-asset.json";
    private static final String CREATE_POLICY_FILE_PATH = RESOURCES_FOLDER + "/create-policy.json";
    private static final String CREATE_CONTRACT_DEFINITION_FILE_PATH = RESOURCES_FOLDER + "/create-contract-definition.json";
    private static final String FETCH_DATASET_FROM_CATALOG_FILE_PATH = RESOURCES_FOLDER + "/get-dataset.json";
    private static final String CONTRACT_OFFER_FILE_PATH = RESOURCES_FOLDER + "/create-contract-request.json";
    private static final String CATALOG_DATASET_ID = "\"odrl:hasPolicy\".'@id'";
    private static final String CONTRACT_OFFER_ID_KEY = "{{contract-offer-id}}";
    private static final String ASSET_ID_KEY = "{{asset-id}}";
    private static final String POLICY_ID_KEY = "{{policy-id}}";
    private static final String ACCESS_POLICY_ID_KEY = "{{access-policy-id}}";
    private static final String CONTRACT_POLICY_ID_KEY = "{{contract-policy-id}}";
    private static final String CONTRACT_DEFINITION_ID_KEY = "{{contract-definition-id}}";
    private static final String RIGHT_OPERAND_KEY = "{{right-operand}}";
    private static final String LEFT_OPERAND_KEY = "{{left-operand}}";
    private static final String ID = "@id";


    public static String createAssetWithId(String assetId) {
        String content = getFileContentFromRelativePath(CREATE_ASSET_FILE_PATH)
                .replace(ASSET_ID_KEY, assetId);
        return post(PrerequisitesCommon.PROVIDER_MANAGEMENT_URL + V3_ASSETS_PATH, content, ID);
    }

    public static void createPolicyWithParams(String policyId, String leftOperand, String rightOperand) {
        String content = getFileContentFromRelativePath(CREATE_POLICY_FILE_PATH)
                .replace(POLICY_ID_KEY, policyId)
                .replace(LEFT_OPERAND_KEY, leftOperand)
                .replace(RIGHT_OPERAND_KEY, rightOperand);
        post(PrerequisitesCommon.PROVIDER_MANAGEMENT_URL + V2_POLICY_DEFINITIONS_PATH, content);
    }


    public static void createContractDefinitionWithParams(String contractId, String accessPolicyId, String contractPolicyId, String assetId) {
        String content = getFileContentFromRelativePath(CREATE_CONTRACT_DEFINITION_FILE_PATH)
                .replace(CONTRACT_DEFINITION_ID_KEY, contractId)
                .replace(ACCESS_POLICY_ID_KEY, accessPolicyId)
                .replace(CONTRACT_POLICY_ID_KEY, contractPolicyId)
                .replace(RIGHT_OPERAND_KEY, assetId);

        post(PrerequisitesCommon.PROVIDER_MANAGEMENT_URL + V2_CONTRACT_DEFINITIONS_PATH, content);
    }

    public static String fetchDatasetFromCatalogWithId(String assetId) {
        String content = getFileContentFromRelativePath(FETCH_DATASET_FROM_CATALOG_FILE_PATH)
                .replace(ASSET_ID_KEY, assetId);
        return post(
                PrerequisitesCommon.CONSUMER_MANAGEMENT_URL + V2_CATALOG_DATASET_REQUEST_PATH, content,
                CATALOG_DATASET_ID
        );
    }

    public static String negotiateContractWithParams(String assetId, String contractOfferId, String leftOperand, String rightOperand) {
        String content = getFileContentFromRelativePath(CONTRACT_OFFER_FILE_PATH)
                .replace(CONTRACT_OFFER_ID_KEY, contractOfferId)
                .replace(LEFT_OPERAND_KEY, leftOperand)
                .replace(RIGHT_OPERAND_KEY, rightOperand)
                .replace(ASSET_ID_KEY, assetId);
        var contractNegotiationId = post(
                PrerequisitesCommon.CONSUMER_MANAGEMENT_URL + V2_CONTRACT_NEGOTIATIONS_PATH,
                content,
                ID
        );
        assertThat(contractNegotiationId).isNotEmpty();
        return contractNegotiationId;
    }

    public static boolean catalogContainsAssetId(String assetId, ArrayList<LinkedHashMap> catalogDatasets) {
        if (catalogDatasets == null || catalogDatasets.isEmpty()) {
            return false;
        }
        for (LinkedHashMap dataset : catalogDatasets) {
            Object idValue = dataset.get("@id");

            if (idValue != null && assetId.equals(idValue.toString())) {
                return true;
            }
        }
        return false;
    }


}
