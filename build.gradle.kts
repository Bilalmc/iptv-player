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
            // OwnTV's pinned dependency stack now requires API 37 at compile time. Keep the
            // runtime target at the upstream value (36) while compiling the whole product against 37.
            compileSdk = 37

            defaultConfig {
                applicationId = providers.gradleProperty("product.applicationId")
                    .orElse("com.bilalmc.iptvplayer")
                    .get()
                versionName = providers.gradleProperty("product.versionName")
                    .orElse("0.1.0")
                    .get()
            }

            // OwnTV's app module is mounted from a git submodule. The manifest is deliberately
            // supplied from the product repository so the launcher identity is ours while all
            // OwnTV services/providers and its internal MainActivity remain available.
            sourceSets.getByName("main") {
                manifest.srcFile(rootProject.file("product-app/src/main/AndroidManifest.xml"))
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
