plugins {
    `java-library`
}

dependencies {
    api(libs.edc.control.plane.spi)
//    api(project(":spi:control-plane:contract-spi"))
//    api(project(":spi:control-plane:transfer-spi"))
//    api(project(":spi:control-plane:asset-spi"))
//
//    implementation(libs.edc.spi.core)
    implementation(libs.edc.data.plane.http)
//    testImplementation(project(":tests:junit-base"))
//
//    testImplementation(project(":core:common:connector-core"))
//    testImplementation(project(":core:control-plane:control-plane-core"))
//    testImplementation(project(":core:common:lib:query-lib"))
}