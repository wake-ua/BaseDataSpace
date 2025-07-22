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
 *       Lucia de Espona University of Alicante
 *
 */

package org.eclipse.edc.heleade.cbm;

import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.heleade.util.HttpRequestLoggerContainer;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.heleade.common.FileTransferCommon.getFileContentFromRelativePath;
import static org.eclipse.edc.heleade.common.NegotiationCommon.createCbmAsset;
import static org.eclipse.edc.heleade.common.NegotiationCommon.createContractDefinition;
import static org.eclipse.edc.heleade.common.NegotiationCommon.createPolicy;
import static org.eclipse.edc.heleade.common.NegotiationCommon.fetchCatalogDatasets;
import static org.eclipse.edc.heleade.common.NegotiationCommon.fetchDatasetFromCatalog;
import static org.eclipse.edc.heleade.common.NegotiationCommon.getContractAgreementId;
import static org.eclipse.edc.heleade.common.NegotiationCommon.negotiateContract;
import static org.eclipse.edc.heleade.common.PrerequisitesCommon.getConsumer;
import static org.eclipse.edc.heleade.common.PrerequisitesCommon.getProvider;
import static org.eclipse.edc.heleade.util.TransferUtil.checkTransferStatus;
import static org.eclipse.edc.heleade.util.TransferUtil.startTransfer;

@EndToEndTest
@Testcontainers
public class CbmProviderTransferTest {
    private static final String CREATE_CBM_DATASET_FILE_PATH = "system-tests/src/test/resources/cbm/create-cbm-dataset.json";
    private static final String CREATE_CBM_DATASET_OPEN_CONTRACT_DEFINITION_FILE_PATH = "system-tests/src/test/resources/cbm/create-cbm-dataset-open-contract-definition.json";
    private static final String CREATE_OPEN_POLICY_FILE_PATH = "system-tests/src/test/resources/sample/create-open-policy.json";
    private static final String CATALOG_REQUEST_FILE_PATH = "system-tests/src/test/resources/sample/fetch-catalog-with-samples.json";
    private static final String NEGOTIATE_CONTRACT_FILE_PATH = "system-tests/src/test/resources/transfer/negotiate-contract.json";
    private static final String FETCH_CBM_DATASET_FROM_CATALOG_FILE_PATH = "system-tests/src/test/resources/cbm/get-cbm-dataset.json";
    private static final String START_TRANSFER_FILE_PATH = "system-tests/src/test/resources/transfer/start-transfer.json";

    @RegisterExtension
    static RuntimeExtension provider = getProvider();

    @RegisterExtension
    static RuntimeExtension consumer = getConsumer();

    @Container
    public static HttpRequestLoggerContainer httpRequestLoggerContainer = new HttpRequestLoggerContainer();

    @Test
    void runCbmNegotiationSteps() {
        var catalogDatasetsEmpty = fetchCatalogDatasets(CATALOG_REQUEST_FILE_PATH);
        assertThat(catalogDatasetsEmpty).isEmpty();

        createCbmAsset(CREATE_CBM_DATASET_FILE_PATH);
        createPolicy(CREATE_OPEN_POLICY_FILE_PATH);
        createContractDefinition(CREATE_CBM_DATASET_OPEN_CONTRACT_DEFINITION_FILE_PATH);
        var catalogDatasetsList = fetchCatalogDatasets(CATALOG_REQUEST_FILE_PATH);
        assertThat(catalogDatasetsList).isNotEmpty();
        assertThat(catalogDatasetsList.size()).isEqualTo(1);
        var catalogDatasetId = fetchDatasetFromCatalog(FETCH_CBM_DATASET_FROM_CATALOG_FILE_PATH);
        var contractNegotiationId = negotiateContract(NEGOTIATE_CONTRACT_FILE_PATH, catalogDatasetId);
        var contractAgreementId = getContractAgreementId(contractNegotiationId);
        assertThat(contractAgreementId).isNotEmpty();

        var port = httpRequestLoggerContainer.getPort();
        var requestBody = getFileContentFromRelativePath(START_TRANSFER_FILE_PATH)
                .replace("4000", String.valueOf(port));
        var transferProcessId = startTransfer(requestBody, contractAgreementId);
        checkTransferStatus(transferProcessId, TransferProcessStates.COMPLETED);
        assertThat(httpRequestLoggerContainer.getLog()).contains("qui est esse");
    }
}
