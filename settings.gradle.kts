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

include(":util:http-request-logger")
include(":system-tests")
include(":federated-catalog")

include(":providers:provider")
include(":providers:provider-template")
include(":providers:provider-base")
