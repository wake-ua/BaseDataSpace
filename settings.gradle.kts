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


include(":util:http-request-logger")
include(":system-tests")

include(":federated-catalog")

include(":providers:provider")
include(":providers:provider-template")
include(":providers:provider-base")

include(":consumers:consumer")
include(":consumers:consumer-base")
