plugins {
    `java-library`
    id("application")
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(libs.edc.ih.spi)
    implementation(libs.edc.ih.spi.did)
    implementation(libs.edc.ih.spi.credential)
    runtimeOnly(libs.edc.bom.identityhub)
    implementation(libs.edc.presentation.api)
    implementation(libs.edc.vault.hashicorp)
    runtimeOnly(libs.edc.bom.identityhub.sql)

}


