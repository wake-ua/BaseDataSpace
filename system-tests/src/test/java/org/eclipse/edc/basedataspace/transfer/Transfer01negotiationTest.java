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
 *       Mercedes-Benz Tech Innovation GmbH - Sample workflow test
 *       Fraunhofer Institute for Software and Systems Engineering - use current ids instead of placeholder
 *
 */

package org.eclipse.edc.basedataspace.transfer;

import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.basedataspace.common.NegotiationCommon.createAsset;
import static org.eclipse.edc.basedataspace.common.NegotiationCommon.createContractDefinition;
import static org.eclipse.edc.basedataspace.common.NegotiationCommon.createPolicy;
import static org.eclipse.edc.basedataspace.common.NegotiationCommon.fetchDatasetFromCatalog;
import static org.eclipse.edc.basedataspace.common.NegotiationCommon.getContractAgreementId;
import static org.eclipse.edc.basedataspace.common.NegotiationCommon.negotiateContract;
import static org.eclipse.edc.basedataspace.common.PrerequisitesCommon.getConsumer;
import static org.eclipse.edc.basedataspace.common.PrerequisitesCommon.getProvider;

@EndToEndTest
public class Transfer01negotiationTest {

    @RegisterExtension
    static RuntimeExtension provider = getProvider();

    @RegisterExtension
    static RuntimeExtension consumer = getConsumer();

    private static final String NEGOTIATE_CONTRACT_FILE_PATH = "system-tests/src/test/resources/negotiate-contract.json";
    private static final String FETCH_DATASET_FROM_CATALOG_FILE_PATH = "system-tests/src/test/resources/get-dataset.json";

    @Test
    void runNegotiationSteps() {
        createAsset();
        createPolicy();
        createContractDefinition();
        var catalogDatasetId = fetchDatasetFromCatalog(FETCH_DATASET_FROM_CATALOG_FILE_PATH);
        var contractNegotiationId = negotiateContract(NEGOTIATE_CONTRACT_FILE_PATH, catalogDatasetId);
        var contractAgreementId = getContractAgreementId(contractNegotiationId);
        assertThat(contractAgreementId).isNotEmpty();
    }
}
