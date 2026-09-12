import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.baselineprofile) apply false
}

// Product identity and integration points are owned by this repository while the Android
// implementation remains sourced from the pinned OwnTV tree.
subprojects {
    plugins.withId("com.android.application") {
        extensions.configure<ApplicationExtension> {
            compileSdk = 37

            defaultConfig {
                applicationId = providers.gradleProperty("product.applicationId")
                    .orElse("com.bilalmc.iptvplayer")
                    .get()
                versionName = providers.gradleProperty("product.versionName")
                    .orElse("0.1.0")
                    .get()
            }
        }

        dependencies.add("implementation", project(":product-ui"))
    }

    plugins.withId("com.android.library") {
        extensions.configure<LibraryExtension> {
            compileSdk = 37
        }
    }
}
