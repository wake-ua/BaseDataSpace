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
    runtimeOnly(libs.edc.fc.core)
    runtimeOnly(libs.edc.fc.ext.api)

    // catalog node directory
    implementation(libs.edc.fc.spi.crawler)

    // standalone fc
    implementation(libs.edc.connector.core)
    runtimeOnly(libs.edc.boot)
    runtimeOnly(libs.edc.control.plane.core)
    implementation(libs.edc.configuration.filesystem)
    runtimeOnly(libs.edc.token.core)
    implementation(libs.edc.http)
    runtimeOnly(libs.edc.dsp)
    implementation(libs.edc.iam.mock)
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

var distTar = tasks.getByName("distTar")
var distZip = tasks.getByName("distZip")

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
    archiveFileName.set("federated-catalog.jar")
    dependsOn(distTar, distZip)
}


