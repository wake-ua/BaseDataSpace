/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

plugins {
    `java-library`
    id("application")
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(libs.jakarta.rsApi)
    implementation(libs.edc.boot)
    implementation(libs.edc.http)
    api(libs.edc.data.plane.spi)
    api(libs.edc.json.ld.spi)
    implementation(project(":providers:policy:claims-checker"));
    implementation(project(":providers:policy:policy-evaluation"));
    implementation(project(":providers:policy:policy-always-true"));
    runtimeOnly(project(":providers:provider"))

}

application {
    mainClass.set("$group.boot.system.runtime.BaseRuntime")
}

var distTar = tasks.getByName("distTar")
var distZip = tasks.getByName("distZip")

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
    archiveFileName.set("provider-ebird.jar")
    dependsOn(distTar, distZip)
}
