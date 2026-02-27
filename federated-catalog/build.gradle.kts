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

plugins {
    `java-library`
    id("application")
    alias(libs.plugins.shadow)
}

dependencies {
    // base fc
    implementation(libs.edc.fc.api)
    implementation(libs.edc.fc.core)
    implementation(project(":commons"))

    // catalog node directory
    implementation(libs.edc.fc.spi.crawler)
    implementation(libs.edc.catalog.util.lib)
    implementation(libs.edc.lib.util)
    implementation(libs.edc.spi.transaction.datasource)


    // consumer fc
    implementation(libs.edc.runtime.core)
    runtimeOnly(libs.edc.boot)
    implementation(libs.edc.http)
    implementation(libs.edc.dsp)
    runtimeOnly(libs.edc.token.core)

    implementation(libs.edc.connector.core)
    implementation(libs.edc.control.plane.core)
    implementation(libs.edc.configuration.filesystem)

    // mongodb cache storage
    implementation(libs.edc.fc.spi.core)
    implementation("org.mongodb:mongodb-driver-sync:5.5.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.16.1")
    implementation(libs.edc.verifiable.credentials.spi)
    implementation(libs.edc.control.plane.transform)

    // consumer
    runtimeOnly(project(":consumers:consumer"))

    // upgrade 14.1
    implementation(libs.edc.dsp.catalog.transform.lib)
//    implementation(libs.edc.spi.transaction)
    implementation(libs.edc.spi.transform)
    implementation(libs.edc.spi.catalog)
//    implementation(libs.edc.spi.core)
    implementation(libs.edc.dsp.catalog.lib)
    implementation(libs.edc.dsp.catalog.http.api.lib)
    implementation(libs.edc.lib.transform)
    implementation(libs.edc.management.api)
}

application {
    mainClass.set("$group.boot.system.runtime.BaseRuntime")
}

var distTar = tasks.getByName("distTar")
var distZip = tasks.getByName("distZip")

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
    archiveFileName.set("federated-catalog.jar")
    dependsOn(distTar, distZip)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}


