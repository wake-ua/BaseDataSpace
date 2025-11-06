rootProject.name = "BaseDataSpace"

pluginManagement {
    repositories {
        mavenLocal()
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
        mavenCentral()
    }
}


include(":util:http-request-logger")
include(":system-tests")

include(":federated-catalog")

include(":consumers:consumer")
include(":consumers:consumer-base")
include("consumers:search-service")

include("providers:content-based-catalog-dispatcher")
include(":providers:provider")
include(":providers:provider-base")
include(":providers:provider-base-prod")
include("providers:provider-ebird")
include("providers:provider-mastral")

include("commons")

include("providers:policy-functions")
include("identity-hub")
include("providers:iam-claims")