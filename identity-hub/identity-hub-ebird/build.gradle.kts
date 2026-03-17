
plugins {
    `java-library`
    id("application")
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(libs.edc.boot)
    implementation(libs.edc.http)
    runtimeOnly(project(":identity-hub:identity-hub"))
}

application {
    mainClass.set("$group.boot.system.runtime.BaseRuntime")
}

var distTar = tasks.getByName("distTar")
var distZip = tasks.getByName("distZip")

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
    archiveFileName.set("identity-hub-ebird.jar")
    dependsOn(distTar, distZip)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}
