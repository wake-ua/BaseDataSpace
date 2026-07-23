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
    runtimeOnly(libs.edc.federatedcatalog.base.bom)
    runtimeOnly(libs.edc.bom.controlplane.base)

    implementation(libs.edc.fc.api)
    implementation(project(":commons"))
    implementation(libs.edc.connector.core)

    // catalog node directory
    implementation(libs.edc.fc.spi.crawler)
    implementation(libs.edc.spi.transaction)
    implementation(libs.edc.spi.transaction.datasource)

    // mongodb cache storage
    implementation(libs.edc.fc.spi.core)
    implementation(libs.mongodb.driver.sync)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype)
    implementation(libs.edc.verifiable.credentials.spi)

    // consumer
    runtimeOnly(project(":consumers:consumer"))

    // upgrade 14.1
    implementation(libs.edc.fc.core.v2025)

    // upgrade 17.0
    implementation(libs.edc.jersey.providers.lib)
    implementation(libs.edc.management.api.lib)

    // verification API
    implementation(libs.edc.jsonld)

    // vocabulary
    implementation(libs.net.sourceforge.owlapi)

    // test
    testImplementation(libs.edc.junit)

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


