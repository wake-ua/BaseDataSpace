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

package org.eclipse.edc.heleade.common;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import io.restassured.http.ContentType;
import org.eclipse.edc.heleade.util.ConfigPropertiesLoader;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.eclipse.edc.heleade.common.FileTransferCommon.getFileContentFromRelativePath;
import static org.eclipse.edc.heleade.common.PrerequisitesCommon.API_KEY_HEADER_KEY;
import static org.eclipse.edc.heleade.common.PrerequisitesCommon.API_KEY_HEADER_VALUE;
import static org.eclipse.edc.heleade.util.ConfigPropertiesLoader.fromPropertiesFile;
import static org.eclipse.edc.heleade.util.TransferUtil.post;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.emptyString;

public class FederatedCatalogCommon {
    public static final String PARTICIPANT_FILE_PATH = "system-tests/src/test/resources/federated-catalog/participant-test.json";
    private static final String EMBEDDED_FC = "federated-catalog";
    private static final String FC_CONFIG_PROPERTIES_FILE_PATH = "system-tests/src/test/resources/fc-test-configuration.properties";

    private static final String CRAWLER_EXECUTION_DELAY = "edc.catalog.cache.execution.delay.seconds";
    public static final int CRAWLER_EXECUTION_DELAY_VALUE = 5;
    private static final String CRAWLER_EXECUTION_PERIOD = "edc.catalog.cache.execution.period.seconds";
    public static final int CRAWLER_EXECUTION_PERIOD_VALUE = 20;
    public static final int TIMEOUT = 2 * CRAWLER_EXECUTION_PERIOD_VALUE;

    public static final String FC_CATALOG_API_ENDPOINT = "http://localhost:59195/api/catalog/v1alpha/catalog/query";
    public static final String FEDERATED_CATALOG_MANAGEMENT_URL = "http://localhost:59193/management";
    public static final String V_NODE_DIRECTORY_PATH = "/v1alpha/directory";
    public static final String EMPTY_QUERY_FILE_PATH = "system-tests/src/test/resources/federated-catalog/empty-query.json";
    public static final String ID_QUERY_FILE_PATH = "system-tests/src/test/resources/federated-catalog/id-query.json";
    public static final String TYPE = "[0].@type";
    public static final String CATALOG = "dcat:Catalog";
    public static final String DATASET_ASSET_ID = "[0].'dcat:dataset'.@id";
    public static final String FC_CACHE_COLLECTION_NAME = "edc_federated_catalog";
    public static final String FC_DIRECTORY_COLLECTION_NAME = "edc_node_directory";

    public static RuntimeExtension getEmbeddedFc(String modulePath) {
        return getRuntime(modulePath, EMBEDDED_FC, FC_CONFIG_PROPERTIES_FILE_PATH);
    }

    private static RuntimeExtension getRuntime(
            String modulePath,
            String moduleName,
            String configPropertiesFilePath
    ) {
        Config config = ConfigPropertiesLoader.fromPropertiesFile(configPropertiesFilePath).get();
        startMongodbCollection(config);
        return new RuntimePerClassExtension(new EmbeddedRuntime(moduleName, modulePath)
                .configurationProvider(fromPropertiesFile(configPropertiesFilePath))
                .configurationProvider(() -> ConfigFactory.fromMap(Map.of(
                        CRAWLER_EXECUTION_DELAY, Integer.toString(CRAWLER_EXECUTION_DELAY_VALUE),
                        CRAWLER_EXECUTION_PERIOD, Integer.toString(CRAWLER_EXECUTION_PERIOD_VALUE)))
                )
        );
    }

    public static void addNodeToDirectory() {
        addNodeToDirectory(PARTICIPANT_FILE_PATH);
    }

    public static void addNodeToDirectory(String filePath) {
        post(FederatedCatalogCommon.FEDERATED_CATALOG_MANAGEMENT_URL + FederatedCatalogCommon.V_NODE_DIRECTORY_PATH,
                getFileContentFromRelativePath(filePath));
    }

    public static String postAndAssertType(String url, String requestBody, String jsonPath) {
        return given()
                .headers(API_KEY_HEADER_KEY, API_KEY_HEADER_VALUE)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post(url)
                .then()
                .log().ifError()
                .statusCode(HttpStatus.SC_OK)
                .body(TYPE, not(emptyString()))
                .body(TYPE, is(CATALOG))
                .extract()
                .jsonPath()
                .get(jsonPath);
    }

    public static void startMongodbCollection(Config config) {
        try {

            // Get parameters
            String uri = config.getString("org.eclipse.edc.heleade.federated.catalog.extension.store.mongodb.uri");
            String db = config.getString("org.eclipse.edc.heleade.federated.catalog.extension.store.mongodb.db");
            String collectionCache = FC_CACHE_COLLECTION_NAME;
            String collectionDirectory = FC_DIRECTORY_COLLECTION_NAME;

            ServerApi serverApi = ServerApi.builder()
                    .version(ServerApiVersion.V1)
                    .build();
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(uri))
                    .serverApi(serverApi)
                    .build();
            MongoClient mongoClient = MongoClients.create(settings);

            mongoClient.getDatabase(db).getCollection(collectionCache).drop();
            mongoClient.getDatabase(db).createCollection(collectionCache);

            mongoClient.getDatabase(db).getCollection(collectionDirectory).drop();
            mongoClient.getDatabase(db).createCollection(collectionDirectory);

            mongoClient.close();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
