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
}

dependencies {
    api(libs.edc.data.plane.spi)
    api(libs.edc.json.ld.spi)

    implementation(libs.edc.control.plane.core)
    implementation(project(":providers:policy:claims-checker"));
    implementation(project(":iam-identity"));

}
