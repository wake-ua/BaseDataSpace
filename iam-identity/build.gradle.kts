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
    api(libs.edc.data.plane.spi)
    api(libs.edc.json.ld.spi)
    implementation(libs.jakarta.rsApi)
    implementation(libs.edc.control.plane.core)
    implementation(libs.edc.http)

}
