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
 *
 */

package org.eclipse.edc.heleade.common;

import org.eclipse.edc.heleade.util.ConfigPropertiesLoader;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.spi.system.configuration.Config;
import org.postgresql.PGProperty;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import static org.eclipse.edc.heleade.common.FileTransferCommon.getFileFromRelativePath;

public class PrerequisitesCommon {
    public static final String API_KEY_HEADER_KEY = "X-Api-Key";
    public static final String API_KEY_HEADER_VALUE = "password";
    public static final String PROVIDER_MANAGEMENT_URL = "http://localhost:49193/management";
    public static final String CONSUMER_MANAGEMENT_URL = "http://localhost:39193/management";

    private static final String PROVIDER_MODULE_PATH = ":providers:provider-base";
    private static final String CONSUMER_MODULE_PATH = ":consumers:consumer-base";
    private static final String PROVIDER = "provider";
    private static final String CONSUMER = "consumer";
    private static final String PROVIDER_CONFIG_PROPERTIES_FILE_PATH = "system-tests/src/test/resources/provider-test-configuration.properties";
    private static final String CONSUMER_CONFIG_PROPERTIES_FILE_PATH = "system-tests/src/test/resources/consumer-test-configuration.properties";
    private static final String PROVIDER_SQL_FILE_PATH = "system-tests/src/test/resources/sql/database.sql";

    public static RuntimeExtension getProvider() {
        return getProvider(PROVIDER_MODULE_PATH, PROVIDER_CONFIG_PROPERTIES_FILE_PATH);
    }

    public static RuntimeExtension getProvider(String modulePath, String configPath) {
        Config config = ConfigPropertiesLoader.fromPropertiesFile(configPath).get();
        startPostgresqlDb(config);
        return getConnector(modulePath, PROVIDER, configPath);
    }

    public static RuntimeExtension getConsumer() {
        return getConsumer(CONSUMER_MODULE_PATH);
    }

    public static RuntimeExtension getConsumer(String modulePath) {
        return getConnector(modulePath, CONSUMER, CONSUMER_CONFIG_PROPERTIES_FILE_PATH);
    }

    public static RuntimeExtension getConnector(
            String modulePath,
            String moduleName,
            String configPropertiesFilePath
    ) {
        return new RuntimePerClassExtension(new EmbeddedRuntime(moduleName, modulePath)
                .configurationProvider(ConfigPropertiesLoader.fromPropertiesFile(configPropertiesFilePath))
        );
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
