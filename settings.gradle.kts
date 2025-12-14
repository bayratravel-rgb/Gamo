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

// --- APPS ---
include(":apps:customer-app")
include(":apps:driver-app")
include(":apps:delivery-app")
include(":apps:hotel-reception-app")

// --- SHARED MODULES ---
include(":packages:shared-types")
include(":packages:api-client")
include(":packages:ui-components")
include(":packages:data-layer")
include(":packages:utils")

// --- BACKEND (Optional for Android Build, but keeping in tree) ---
// include(":backend:ktor-server") 
