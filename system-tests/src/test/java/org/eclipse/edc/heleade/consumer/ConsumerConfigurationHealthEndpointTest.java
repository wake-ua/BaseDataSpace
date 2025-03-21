/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.heleade.consumer;

import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static io.restassured.RestAssured.given;
import static org.eclipse.edc.heleade.util.ConfigPropertiesLoader.fromPropertiesFile;
import static org.hamcrest.CoreMatchers.containsString;

@EndToEndTest
class ConsumerConfigurationHealthEndpointTest {

    @RegisterExtension
    static RuntimeExtension controlPlane = new RuntimePerClassExtension(new EmbeddedRuntime(
            "consumer",
            ":consumers:consumer-base"
    ).configurationProvider(fromPropertiesFile("system-tests/src/test/resources/consumer-test-configuration.properties")));

    @Test
    void testConsumerHealthEndpoint() {
        given()
                .get("http://localhost:39191/api/health")
                .then()
                .statusCode(200)
                .body(containsString("alive"));
    }
}
