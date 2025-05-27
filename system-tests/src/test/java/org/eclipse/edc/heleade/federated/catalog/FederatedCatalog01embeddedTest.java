/*
 *  Copyright (c) 2024 Fraunhofer-Gesellschaft
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft - initial API and implementation
 *
 */

package org.eclipse.edc.heleade.federated.catalog;

import jakarta.json.JsonArray;
import org.eclipse.edc.heleade.common.FederatedCatalogCommon;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Clock;
import java.time.Duration;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.heleade.common.FederatedCatalogCommon.CRAWLER_EXECUTION_DELAY_VALUE;
import static org.eclipse.edc.heleade.common.FederatedCatalogCommon.DATASET_ASSET_ID;
import static org.eclipse.edc.heleade.common.FederatedCatalogCommon.EMPTY_QUERY_FILE_PATH;
import static org.eclipse.edc.heleade.common.FederatedCatalogCommon.FC_CATALOG_API_ENDPOINT;
import static org.eclipse.edc.heleade.common.FederatedCatalogCommon.TIMEOUT;
import static org.eclipse.edc.heleade.common.FederatedCatalogCommon.getEmbeddedFc;
import static org.eclipse.edc.heleade.common.FederatedCatalogCommon.postAndAssertType;
import static org.eclipse.edc.heleade.common.FileTransferCommon.getFileContentFromRelativePath;
import static org.eclipse.edc.heleade.common.NegotiationCommon.createAsset;
import static org.eclipse.edc.heleade.common.NegotiationCommon.createContractDefinition;
import static org.eclipse.edc.heleade.common.NegotiationCommon.createPolicy;
import static org.eclipse.edc.heleade.common.PrerequisitesCommon.getProvider;
import static org.eclipse.edc.heleade.util.TransferUtil.getAsJsonArray;
import static org.eclipse.edc.heleade.util.TransferUtil.getResponseBody;
import static org.eclipse.edc.heleade.util.TransferUtil.post;
import static org.hamcrest.CoreMatchers.containsString;

@EndToEndTest
public class FederatedCatalog01embeddedTest {

    @RegisterExtension
    static final RuntimeExtension PARTICIPANT_CONNECTOR = getProvider();

    @RegisterExtension
    static final RuntimeExtension FC_RUNTIME = getEmbeddedFc(":federated-catalog");

    @Test
    void shouldStartFederatedCatalogRuntimesTest() {
        assertThat(PARTICIPANT_CONNECTOR.getService(Clock.class)).isNotNull();
        assertThat(FC_RUNTIME.getService(Clock.class)).isNotNull();
    }

    @Test
    void testFederatedCatalogHealthEndpoint() {
        given()
                .get("http://localhost:59191/api/health-fc")
                .then()
                .statusCode(200)
                .body(containsString("alive"));
    }

    @Test
    void testFederatedCatalogConsumerHealthEndpoint() {
        given()
                .get("http://localhost:59191/api/health")
                .then()
                .statusCode(200)
                .body(containsString("alive"));
    }

    @Test
    void runFederatedCatalogStepsTest() {
        String assetId = createAsset();
        createPolicy();
        createContractDefinition();

        post(FederatedCatalogCommon.FEDERATED_CATALOG_MANAGEMENT_URL + FederatedCatalogCommon.V_NODE_DIRECTORY_PATH,
                getFileContentFromRelativePath(FederatedCatalogCommon.PARTICIPANT_FILE_PATH));

        JsonArray nodeDirectory = getAsJsonArray(getResponseBody(FederatedCatalogCommon.FEDERATED_CATALOG_MANAGEMENT_URL  + FederatedCatalogCommon.V_NODE_DIRECTORY_PATH));
        assertThat(nodeDirectory).isNotNull();
        assertThat(nodeDirectory.size()).isGreaterThanOrEqualTo(1);

        // call catalog API from standalone FC
        await()
                .atMost(Duration.ofSeconds(TIMEOUT))
                .pollDelay(Duration.ofSeconds(CRAWLER_EXECUTION_DELAY_VALUE))
                .ignoreExceptions()
                .until(() -> postAndAssertType(FC_CATALOG_API_ENDPOINT, getFileContentFromRelativePath(EMPTY_QUERY_FILE_PATH), DATASET_ASSET_ID),
                        id -> id.equals(assetId));
    }
}
