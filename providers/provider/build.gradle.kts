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
    implementation(libs.edc.boot)
    implementation(libs.edc.api.lib)
    implementation(libs.edc.connector.core)
    implementation(libs.jakarta.rsApi)

    implementation(libs.edc.control.plane.transform)
    implementation(libs.edc.control.api.configuration)
    implementation(libs.edc.control.plane.api.client)
    implementation(libs.edc.control.plane.api)
    implementation(libs.edc.control.plane.core)
    implementation(libs.edc.control.plane.catalog)
    implementation(libs.edc.control.plane.services)
    implementation(libs.edc.control.plane.spi)

    implementation(libs.edc.catalog.spi)
    implementation(libs.edc.token.core)
    implementation(libs.edc.dsp)
    implementation(libs.edc.http)
    implementation(libs.edc.jersey.providers.lib)
    implementation(libs.edc.configuration.filesystem)
   // implementation(libs.edc.iam.mock)
    implementation(libs.edc.management.api)
    implementation(libs.edc.transfer.data.plane.signaling)
    implementation(libs.edc.validator.data.address.http.data)
    implementation(libs.edc.lib.validator)
    implementation(libs.edc.lib.json)
    implementation(libs.edc.verifiable.credentials.spi)
    implementation(project(":providers:content-based-catalog-dispatcher"))
    implementation(project(":commons"))

    implementation(libs.edc.edr.cache.api)
    implementation(libs.edc.edr.store.core)
    implementation(libs.edc.edr.store.receiver)

    implementation(libs.edc.data.plane.selector.api)
    implementation(libs.edc.data.plane.selector.core)

    implementation(libs.edc.data.plane.self.registration)
    implementation(libs.edc.data.plane.signaling.api)
    implementation(libs.edc.data.plane.public.api)
    implementation(libs.edc.data.plane.core)
    implementation(libs.edc.data.plane.http)
    implementation(libs.edc.data.plane.iam)
    implementation(libs.edc.control.api.configuration)

    implementation("com.networknt:json-schema-validator:1.5.6")

    // auth
    implementation(libs.edc.auth.tokenbased)
    implementation(libs.edc.spi.auth)
    implementation(libs.edc.auth.configuration)
    

    // sql storage
    implementation(libs.edc.spi.transaction.datasource)
    implementation(libs.bundles.edc.sqlstores)
    implementation(libs.edc.transaction.local)
    implementation(libs.edc.sql.pool)
    implementation(libs.edc.sql.core)
    implementation(libs.postgres)

    // identification
    implementation(project(":iam-identity"));
    implementation(project(":providers:policy:policy-evaluation"));
    implementation(project(":providers:policy:policy-always-true"));

    // test
    testImplementation(libs.restAssured)
    testImplementation(libs.awaitility)
    testImplementation(libs.edc.junit)

}
