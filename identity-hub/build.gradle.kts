plugins {
    `java-library`
    id("application")
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(libs.edc.bom.identityhub)

    implementation(libs.edc.ih.spi) // needed in the extensions here
    implementation(libs.edc.ih.spi.did) // needed in the extensions here
    implementation(libs.edc.ih.spi.credential) // needed in the extensions here
}

application {
    mainClass.set("$group.boot.system.runtime.BaseRuntime")
}

var distTar = tasks.getByName("distTar")
var distZip = tasks.getByName("distZip")

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xml")
    mergeServiceFiles()
    archiveFileName.set("identity-hub.jar")
    dependsOn(distTar, distZip)
}
