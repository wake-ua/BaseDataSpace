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
import org.eclipse.edc.heleade.util.ConfigPropertiesLoader;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.postgresql.PGProperty;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import static io.restassured.RestAssured.given;
import static org.eclipse.edc.heleade.common.FileTransferCommon.getFileFromRelativePath;
import static org.eclipse.edc.heleade.common.PrerequisitesCommon.API_KEY_HEADER_KEY;
import static org.eclipse.edc.heleade.common.PrerequisitesCommon.API_KEY_HEADER_VALUE;
import static org.eclipse.edc.heleade.util.ConfigPropertiesLoader.fromPropertiesFile;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.emptyString;

public class FederatedCatalogCommon {
    private static final String EMBEDDED_FC = "federated-catalog";
    private static final String FC_CONFIG_PROPERTIES_FILE_PATH = "system-tests/src/test/resources/fc-test-configuration.properties";
    private static final String PROVIDER_SQL_FILE_PATH = "system-tests/src/test/resources/sql/database.sql";
    private static final String FC_SQL_FILE_PATH = "system-tests/src/test/resources/sql/fc-database.sql";

    private static final String CRAWLER_EXECUTION_DELAY = "edc.catalog.cache.execution.delay.seconds";
    public static final int CRAWLER_EXECUTION_DELAY_VALUE = 5;
    private static final String CRAWLER_EXECUTION_PERIOD = "edc.catalog.cache.execution.period.seconds";
    public static final int CRAWLER_EXECUTION_PERIOD_VALUE = 40;
    public static final int TIMEOUT = 5 * CRAWLER_EXECUTION_PERIOD_VALUE;

    public static final String FC_CATALOG_API_ENDPOINT = "http://localhost:59195/api/catalog/v1alpha/catalog/query";
    public static final String EMPTY_QUERY_FILE_PATH = "system-tests/src/test/resources/federated-catalog/empty-query.json";
    public static final String TYPE = "[0].@type";
    public static final String CATALOG = "dcat:Catalog";
    public static final String DATASET_ASSET_ID = "[0].'dcat:dataset'.@id";

    public static RuntimeExtension getEmbeddedFc(String modulePath) {
        return getRuntime(modulePath, EMBEDDED_FC, FC_CONFIG_PROPERTIES_FILE_PATH);
    }

    private static RuntimeExtension getRuntime(
            String modulePath,
            String moduleName,
            String configPropertiesFilePath
    ) {
        Config config = ConfigPropertiesLoader.fromPropertiesFile(configPropertiesFilePath).get();
        startPostgresqlDb(config);
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

    public static void startPostgresqlDb(Config config) {
        try {

            // Get parameters
            String username = config.getString("edc.datasource.default.user");
            String password = config.getString("edc.datasource.default.password");
            String jdbcUrl = config.getString("edc.datasource.default.url");

            Properties props = org.postgresql.Driver.parseURL(jdbcUrl, null);
            String host = props.getProperty(PGProperty.PG_HOST.getName());
            String port = props.getProperty(PGProperty.PG_PORT.getName());
            String name = props.getProperty(PGProperty.PG_DBNAME.getName());
            String urlNoDb = "jdbc:postgresql://" + host + ":" + port + "/";

            // get DB creation script
            var filePath = getFileFromRelativePath(PROVIDER_SQL_FILE_PATH).toURI();
            var stream = filePath.toURL().openStream();
            String sqlScript = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            // get DB creation script
            var filePathFc = getFileFromRelativePath(FC_SQL_FILE_PATH).toURI();
            var streamFc = filePathFc.toURL().openStream();
            sqlScript += new String(streamFc.readAllBytes(), StandardCharsets.UTF_8);


            // Connect to the database
            Class.forName("org.postgresql.Driver");

            // get empty database
            Connection connectionAux = DriverManager.getConnection(urlNoDb, username, password);
            int dropResult = connectionAux.createStatement().executeUpdate("DROP DATABASE IF EXISTS " + name);
            int createResult = connectionAux.createStatement().executeUpdate(" CREATE DATABASE " + name + " WITH ENCODING = 'UTF8';");
            connectionAux.close();

            Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
            connection.prepareStatement(sqlScript).execute();
            connection.close();

        } catch (ClassNotFoundException | NullPointerException | SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
