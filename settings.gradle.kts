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

include(":providers:provider")
include(":providers:provider-template")
include(":providers:provider-base")
include("providers:provider-ebird")

include(":consumers:consumer")
include(":consumers:consumer-base")

include("providers:control-plane-catalog")
include("providers:control-plane-core")
include("providers:data-address-http-data-spi")
include("providers:catalog-spi")
include("providers:control-plane-spi")
include("providers:dsp-catalog")
include("providers:content-based-ctalog-dispatcher")
include("providers:content-based-catalog-dispatcher")