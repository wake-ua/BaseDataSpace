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
include("system-tests")
include("federated-catalog")
include("federated-catalog")
include("provider-template")
