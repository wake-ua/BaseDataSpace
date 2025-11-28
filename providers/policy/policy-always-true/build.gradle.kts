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
 *       M.O
 *
 */

plugins {
    `java-library`
    id("application")
}

dependencies {
    implementation(libs.edc.catalog.spi)
    implementation(libs.edc.control.plane.spi)
    implementation(libs.edc.policy.engine.spi)
    //implementation(libs.edc.policy.spi)
}