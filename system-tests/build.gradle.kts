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
 *       Fraunhofer-Gesellschaft - dependencies for Federated Catalog Tests
 *       Fraunhofer-Gesellschaft - set working directory to project directory
 *
 */

plugins {
    `java-library`
}

// may not be all necessary (review)
dependencies {
    testImplementation(libs.edc.junit)
    testImplementation(libs.edc.json.ld.lib)
    testImplementation(libs.edc.json.ld.spi)
    testImplementation(libs.edc.control.plane.spi)
    testImplementation(testFixtures(libs.edc.management.api.test.fixtures))
    testImplementation(libs.awaitility)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.restAssured)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.kafka.clients)
    testImplementation(libs.testcontainers.minio)
    testImplementation(libs.testcontainers.hashicorp.vault)
    testImplementation(libs.azure.storage.blob)
    testImplementation(libs.minio.io)
    testImplementation("org.json:json:20250107")

    // runtimes
    testCompileOnly(project(":consumer"))
    testCompileOnly(project(":provider"))

}

//tasks.compileJava {
//    dependsOn(":advanced:advanced-01-open-telemetry:open-telemetry-runtime:build")
//}
