plugins {
    `java-library`
}

dependencies {
//    api(libs.edc.control.plane.spi)
    api(libs.edc.asset.spi)
    api(libs.edc.transfer.spi)
    api(libs.edc.contract.spi)
    api(libs.edc.catalog.spi)
//    api(project(":spi:control-plane:contract-spi"))
//    api(project(":spi:control-plane:transfer-spi"))
//    api(project(":spi:control-plane:asset-spi"))
//
//    implementation(libs.edc.data.plane.http)
//    implementation(project(":spi:common:data-address:data-address-http-data-spi"))
    implementation(project(":providers:data-address-http-data-spi"))
//    implementation(libs.edc.dsp.catalog.http.api)

//    testImplementation(project(":tests:junit-base"))
//
//    testImplementation(project(":core:common:connector-core"))
//    testImplementation(project(":core:control-plane:control-plane-core"))
//    testImplementation(project(":core:common:lib:query-lib"))
}