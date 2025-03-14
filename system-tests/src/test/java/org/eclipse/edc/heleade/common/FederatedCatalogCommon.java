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
import io.restassured.http.ContentType;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.eclipse.edc.heleade.common.PrerequisitesCommon.API_KEY_HEADER_KEY;
import static org.eclipse.edc.heleade.common.PrerequisitesCommon.API_KEY_HEADER_VALUE;
import static org.eclipse.edc.heleade.util.ConfigPropertiesLoader.fromPropertiesFile;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.emptyString;

public class FederatedCatalogCommon {
    private static final String STANDALONE_FC = "federated-catalog";
    private static final String STANDALONE_FC_CONFIG_PROPERTIES_FILE_PATH = "system-tests/src/test/resources/fc-test-configuration.properties";

    private static final String CRAWLER_EXECUTION_DELAY = "edc.catalog.cache.execution.delay.seconds";
    public static final int CRAWLER_EXECUTION_DELAY_VALUE = 5;
    private static final String CRAWLER_EXECUTION_PERIOD = "edc.catalog.cache.execution.period.seconds";
    public static final int CRAWLER_EXECUTION_PERIOD_VALUE = 40;
    public static final int TIMEOUT = 5 * CRAWLER_EXECUTION_PERIOD_VALUE;

    public static final String STANDALONE_FC_CATALOG_API_ENDPOINT = "http://localhost:59195/api/catalog/v1alpha/catalog/query";
    public static final String EMPTY_QUERY_FILE_PATH = "system-tests/src/test/resources/federated-catalog/empty-query.json";
    public static final String TYPE = "[0].@type";
    public static final String CATALOG = "dcat:Catalog";
    public static final String DATASET_ASSET_ID = "[0].'dcat:dataset'.@id";

    public static RuntimeExtension getStandaloneFc(String modulePath) {
        return getRuntime(modulePath, STANDALONE_FC, STANDALONE_FC_CONFIG_PROPERTIES_FILE_PATH);
    }

    private static RuntimeExtension getRuntime(
            String modulePath,
            String moduleName,
            String configPropertiesFilePath
    ) {
        return new RuntimePerClassExtension(new EmbeddedRuntime(moduleName, modulePath)
                .configurationProvider(fromPropertiesFile(configPropertiesFilePath))
                .configurationProvider(() -> ConfigFactory.fromMap(Map.of(
                        CRAWLER_EXECUTION_DELAY, Integer.toString(CRAWLER_EXECUTION_DELAY_VALUE),
                        CRAWLER_EXECUTION_PERIOD, Integer.toString(CRAWLER_EXECUTION_PERIOD_VALUE)))
                )
        );
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

}
