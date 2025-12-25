pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "BayeraTravel"

// APPS
include(":apps:customer-app")
include(":apps:driver-app")
include(":apps:delivery-app")
include(":apps:hotel-reception-app")
include(":apps:merchant-app")

// PACKAGES
include(":packages:shared-types")
include(":packages:utils")
include(":packages:data-layer")
include(":packages:api-client")
include(":packages:ui-components")
