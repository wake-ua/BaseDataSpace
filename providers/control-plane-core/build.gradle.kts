plugins {
    `java-library`
}


dependencies {
//    api(project(":spi:control-plane:asset-spi"))
    api(libs.edc.control.plane.spi)
//    api(project(":spi:control-plane:control-plane-spi"))
//
//    implementation(project(":core:common:lib:store-lib"))
    implementation(libs.edc.lib.store)

//    implementation(project(":core:common:boot"))
//    implementation(project(":core:control-plane:control-plane-catalog"))
//    implementation(project(":core:control-plane:control-plane-contract"))
//    implementation(project(":core:control-plane:control-plane-transfer"))
    implementation(libs.edc.control.plane.transfer)
    implementation(libs.edc.controlplane.services)
    implementation(libs.edc.control.plane.api)
//    implementation(project(":core:control-plane:control-plane-aggregate-services"))
//    implementation(project(":core:common:lib:util-lib"))
//    implementation(project(":core:common:lib:policy-engine-lib"))
//    implementation(project(":core:common:lib:query-lib"))
    implementation(libs.edc.lib.query)
//
//    testImplementation(testFixtures(project(":spi:control-plane:asset-spi")))
//    testImplementation(testFixtures(project(":spi:control-plane:contract-spi")))
//    testImplementation(testFixtures(project(":spi:control-plane:policy-spi")))
//    testImplementation(testFixtures(project(":spi:control-plane:transfer-spi")))
}