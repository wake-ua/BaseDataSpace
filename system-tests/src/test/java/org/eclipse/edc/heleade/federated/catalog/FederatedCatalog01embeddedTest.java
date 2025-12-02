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

import io.restassured.path.json.JsonPath;
import jakarta.json.JsonArray;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Clock;
import java.time.Duration;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.heleade.common.FederatedCatalogCommon.CATALOG;
import static org.eclipse.edc.heleade.common.FederatedCatalogCommon.CRAWLER_EXECUTION_DELAY_VALUE;
import static org.eclipse.edc.heleade.common.FederatedCatalogCommon.DATASET;
import static org.eclipse.edc.heleade.common.FederatedCatalogCommon.DATASET_ASSET_ID;
import static org.eclipse.edc.heleade.common.FederatedCatalogCommon.EMPTY_QUERY_FILE_PATH;
import static org.eclipse.edc.heleade.common.FederatedCatalogCommon.FC_CATALOG_API_ENDPOINT;
import static org.eclipse.edc.heleade.common.FederatedCatalogCommon.FC_CBM_CATALOG_API_ENDPOINT;
import static org.eclipse.edc.heleade.common.FederatedCatalogCommon.FC_CBM_DATASET_API_ENDPOINT;
import static org.eclipse.edc.heleade.common.FederatedCatalogCommon.FEDERATED_CATALOG_MANAGEMENT_URL;
import static org.eclipse.edc.heleade.common.FederatedCatalogCommon.ID_QUERY_FILE_PATH;
import static org.eclipse.edc.heleade.common.FederatedCatalogCommon.TIMEOUT;
import static org.eclipse.edc.heleade.common.FederatedCatalogCommon.V_NODE_DIRECTORY_PATH;
import static org.eclipse.edc.heleade.common.FederatedCatalogCommon.addNodeToDirectory;
import static org.eclipse.edc.heleade.common.FederatedCatalogCommon.getEmbeddedFc;
import static org.eclipse.edc.heleade.common.FederatedCatalogCommon.postAndAssertType;
import static org.eclipse.edc.heleade.common.FileTransferCommon.getFileContentFromRelativePath;
import static org.eclipse.edc.heleade.common.NegotiationCommon.createAsset;
import static org.eclipse.edc.heleade.common.NegotiationCommon.createAssetWithId;
import static org.eclipse.edc.heleade.common.NegotiationCommon.createContractDefinition;
import static org.eclipse.edc.heleade.common.NegotiationCommon.createPolicy;
import static org.eclipse.edc.heleade.common.NegotiationCommon.deleteAsset;
import static org.eclipse.edc.heleade.common.PrerequisitesCommon.getProvider;
import static org.eclipse.edc.heleade.util.TransferUtil.delete;
import static org.eclipse.edc.heleade.util.TransferUtil.getAsJsonArray;
import static org.eclipse.edc.heleade.util.TransferUtil.getResponseBody;
import static org.eclipse.edc.heleade.util.TransferUtil.postJson;
import static org.hamcrest.CoreMatchers.containsString;

@EndToEndTest
public class FederatedCatalog01embeddedTest {

    @RegisterExtension
    static final RuntimeExtension PARTICIPANT_CONNECTOR = getProvider();

    @RegisterExtension
    static final RuntimeExtension FC_RUNTIME = getEmbeddedFc(":federated-catalog");

    static String baseAssetId;
    static final String PROVIDER_EXTRA_FILE_PATH = "system-tests/src/test/resources/federated-catalog/participant-test-directory.json";

    @BeforeAll
    static void beforeAll() {
        baseAssetId = createAsset();
        createPolicy();
        createContractDefinition();
        addNodeToDirectory();
    }

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
    void nodeDirectoryApiTest() {
        String nodeIdPath = "/provider-extra";
        addNodeToDirectory(PROVIDER_EXTRA_FILE_PATH);

        JsonArray nodeDirectory = getAsJsonArray(getResponseBody(FEDERATED_CATALOG_MANAGEMENT_URL  + V_NODE_DIRECTORY_PATH));
        assertThat(nodeDirectory).isNotNull();
        assertThat(nodeDirectory.size()).isEqualTo(2);

        delete(FEDERATED_CATALOG_MANAGEMENT_URL + V_NODE_DIRECTORY_PATH + nodeIdPath);

        assertThat(getAsJsonArray(getResponseBody(FEDERATED_CATALOG_MANAGEMENT_URL  + V_NODE_DIRECTORY_PATH))
                .size()).isEqualTo(1);
    }

    @Test
    void runFederatedCatalogBaseTest() {

        // call catalog API from standalone FC
        await()
                .atMost(Duration.ofSeconds(TIMEOUT))
                .pollDelay(Duration.ofSeconds(CRAWLER_EXECUTION_DELAY_VALUE))
                .ignoreExceptions()
                .until(() -> postAndAssertType(FC_CATALOG_API_ENDPOINT, getFileContentFromRelativePath(EMPTY_QUERY_FILE_PATH), DATASET_ASSET_ID, CATALOG),
                        id -> id.equals(baseAssetId));
    }

    @Test
    void runFederatedCatalogCbmBaseTest() {

        // call catalog API from standalone FC
        await()
                .atMost(Duration.ofSeconds(TIMEOUT))
                .pollDelay(Duration.ofSeconds(CRAWLER_EXECUTION_DELAY_VALUE))
                .ignoreExceptions()
                .until(() -> postAndAssertType(FC_CBM_CATALOG_API_ENDPOINT, getFileContentFromRelativePath(EMPTY_QUERY_FILE_PATH), DATASET_ASSET_ID, CATALOG),
                        id -> id.equals(baseAssetId));
    }

    @Test
    void runFederatedCatalogCbmDatasetEndpointTest() {

        // call catalog API from standalone FC
        await()
                .atMost(Duration.ofSeconds(TIMEOUT))
                .pollDelay(Duration.ofSeconds(CRAWLER_EXECUTION_DELAY_VALUE))
                .ignoreExceptions()
                .until(() -> postAndAssertType(FC_CBM_DATASET_API_ENDPOINT, getFileContentFromRelativePath(EMPTY_QUERY_FILE_PATH), "[0].@id", DATASET),
                        id -> id.equals(baseAssetId));
    }

    @Test
    void nodeMongodbCacheStoreEmptyQueryTest() {
        String assetId1 = createAssetWithId("ApiTestId1");
        String assetId2 = createAssetWithId("ApiTestId2");

        // call catalog API from standalone FC (multiple datasets -  as list)
        await()
                .atMost(Duration.ofSeconds(TIMEOUT))
                .pollDelay(Duration.ofSeconds(CRAWLER_EXECUTION_DELAY_VALUE))
                .ignoreExceptions()
                .until(() -> postAndAssertType(FC_CATALOG_API_ENDPOINT, getFileContentFromRelativePath(EMPTY_QUERY_FILE_PATH), "[0].'dcat:dataset'[0].'@id'", CATALOG),
                        id -> !(id.isEmpty()));

        // get all the datasets
        JsonPath resultJsonPath = postJson(FC_CATALOG_API_ENDPOINT, getFileContentFromRelativePath(EMPTY_QUERY_FILE_PATH));
        assertThat(resultJsonPath).isNotNull();
        var datasets = resultJsonPath.getList("[0].'dcat:dataset'");
        assertThat(datasets.size()).isEqualTo(3);

        // delete and check again
        deleteAsset(assetId1);
        deleteAsset(assetId2);

        // call catalog API from standalone FC (single dataset - as object)
        await()
                .atMost(Duration.ofSeconds(TIMEOUT))
                .pollDelay(Duration.ofSeconds(CRAWLER_EXECUTION_DELAY_VALUE))
                .ignoreExceptions()
                .until(() -> postAndAssertType(FC_CATALOG_API_ENDPOINT, getFileContentFromRelativePath(EMPTY_QUERY_FILE_PATH), DATASET_ASSET_ID, CATALOG),
                        id -> id.equals(baseAssetId));
    }

    @Test
    void nodeMongodbCacheStoreIdQueryTest() {
        String assetId1 = createAssetWithId("ApiTestId1");
        String assetId2 = createAssetWithId("ApiTestId2");

        // call catalog API from standalone FC (one dataset -  as object)
        await()
                .atMost(Duration.ofSeconds(TIMEOUT))
                .pollDelay(Duration.ofSeconds(CRAWLER_EXECUTION_DELAY_VALUE))
                .ignoreExceptions()
                .until(() -> postAndAssertType(FC_CATALOG_API_ENDPOINT, getFileContentFromRelativePath(ID_QUERY_FILE_PATH), DATASET_ASSET_ID, CATALOG),
                        id -> id.equals(assetId2));

        // delete and check again
        deleteAsset(assetId1);
        deleteAsset(assetId2);

        // call catalog API from standalone FC (single dataset - as object)
        await()
                .atMost(Duration.ofSeconds(TIMEOUT))
                .pollDelay(Duration.ofSeconds(CRAWLER_EXECUTION_DELAY_VALUE))
                .ignoreExceptions()
                .until(() -> postAndAssertType(FC_CATALOG_API_ENDPOINT, getFileContentFromRelativePath(EMPTY_QUERY_FILE_PATH), DATASET_ASSET_ID, CATALOG),
                        id -> id.equals(baseAssetId));
    }
}
