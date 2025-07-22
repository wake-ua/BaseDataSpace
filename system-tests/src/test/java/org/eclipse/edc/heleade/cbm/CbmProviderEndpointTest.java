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

import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.heleade.common.NegotiationCommon.createCbmAsset;
import static org.eclipse.edc.heleade.common.NegotiationCommon.createCbmAssetExpect;
import static org.eclipse.edc.heleade.common.NegotiationCommon.createContractDefinition;
import static org.eclipse.edc.heleade.common.NegotiationCommon.createPolicy;
import static org.eclipse.edc.heleade.common.NegotiationCommon.fetchCatalogDatasets;
import static org.eclipse.edc.heleade.common.PrerequisitesCommon.getConsumer;
import static org.eclipse.edc.heleade.common.PrerequisitesCommon.getProvider;

@EndToEndTest
public class CbmProviderEndpointTest {
    private static final String CREATE_CBM_ORIGINAL_DATASET_FILE_PATH = "system-tests/src/test/resources/cbm/create-cbm-dataset-original.json";
    private static final String CREATE_CBM_SAMPLE_FILE_PATH = "system-tests/src/test/resources/cbm/create-cbm-sample.json";
    private static final String CREATE_CBM_BAD_DATASET_FILE_PATH = "system-tests/src/test/resources/cbm/create-cbm-bad-dataset.json";
    private static final String CREATE_CBM_OPEN_CONTRACT_DEFINITION_FILE_PATH = "system-tests/src/test/resources/cbm/create-cbm-open-contract-definition.json";
    private static final String CREATE_CBM_RESTRICTED_CONTRACT_DEFINITION_FILE_PATH = "system-tests/src/test/resources/cbm/create-cbm-restricted-contract-definition.json";
    private static final String CREATE_RESTRICTED_POLICY_FILE_PATH = "system-tests/src/test/resources/sample/create-restricted-policy.json";
    private static final String CREATE_OPEN_POLICY_FILE_PATH = "system-tests/src/test/resources/sample/create-open-policy.json";
    private static final String CATALOG_REQUEST_FILE_PATH = "system-tests/src/test/resources/sample/fetch-catalog-with-samples.json";

    @RegisterExtension
    static RuntimeExtension provider = getProvider();

    @RegisterExtension
    static RuntimeExtension consumer = getConsumer();

    @Test
    void testCreateCbmDatasetWithSample() {
        var catalogDatasetsEmpty = fetchCatalogDatasets(CATALOG_REQUEST_FILE_PATH);
        assertThat(catalogDatasetsEmpty).isEmpty();

        createCbmAsset(CREATE_CBM_ORIGINAL_DATASET_FILE_PATH);
        createPolicy(CREATE_RESTRICTED_POLICY_FILE_PATH);
        createContractDefinition(CREATE_CBM_RESTRICTED_CONTRACT_DEFINITION_FILE_PATH);
        var catalogDatasetsSingle = fetchCatalogDatasets(CATALOG_REQUEST_FILE_PATH);
        assertThat(catalogDatasetsSingle).isNotEmpty();
        assertThat(catalogDatasetsSingle.size()).isEqualTo(1);

        createCbmAsset(CREATE_CBM_SAMPLE_FILE_PATH);
        createPolicy(CREATE_OPEN_POLICY_FILE_PATH);
        createContractDefinition(CREATE_CBM_OPEN_CONTRACT_DEFINITION_FILE_PATH);
        var catalogDatasetsList = fetchCatalogDatasets(CATALOG_REQUEST_FILE_PATH);
        assertThat(catalogDatasetsList).isNotEmpty();
        assertThat(catalogDatasetsList.size()).isEqualTo(2);
    }

    @Test
    void testCreateCbmDatasetValidationShouldFail() {
        int originalCatalogSize = fetchCatalogDatasets(CATALOG_REQUEST_FILE_PATH).size();
        createCbmAssetExpect(CREATE_CBM_BAD_DATASET_FILE_PATH, SC_BAD_REQUEST);
        var catalogDatasets = fetchCatalogDatasets(CATALOG_REQUEST_FILE_PATH);
        assertThat(catalogDatasets.size()).isEqualTo(originalCatalogSize);

    }
}
