/*
 *  Copyright (c) 2025 Universidad de Alicante
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       MO - Universidad de Alicante - initial implementation
 *
 */
plugins {
    `java-library`
    id("application")
}

dependencies {
    implementation(libs.edc.http)
    implementation(libs.edc.spi.core)
    implementation(libs.edc.spi.protocol)
    implementation(project(":commons"))
}
