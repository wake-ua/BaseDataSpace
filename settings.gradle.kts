rootProject.name = "BaseDataSpace"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        mavenLocal()
    }
}

include(":consumer")
include(":provider")

include(":util:http-request-logger")
