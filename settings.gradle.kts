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

include(":providers:content-based-catalog-dispatcher")
include(":providers:provider")
include(":providers:provider-base")
include(":providers:provider-ebird")

include("commons")


include("identity-hub")
include("iam-identity")
include("providers:policy")
include("providers:policy:policy-always-true")
include("providers:policy:policy-evaluation")