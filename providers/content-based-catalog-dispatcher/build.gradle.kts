/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

plugins {
    `java-library`
    id(libs.plugins.swagger.get().pluginId)
}

dependencies {
    api(libs.edc.dsp.catalog)
    api(libs.edc.dsp)
    api(libs.edc.jsonld)
    api(libs.edc.catalog.spi)
    api(libs.edc.catalog.api)
    api(libs.edc.dsp.catalog.http.api)

    implementation(libs.edc.api.core)
    implementation(libs.edc.control.plane.api)
    implementation(libs.edc.boot)
    implementation(libs.edc.http)
    implementation(libs.jakarta.rsApi)

}
