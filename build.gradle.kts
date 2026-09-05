import com.android.build.api.dsl.ApplicationExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.baselineprofile) apply false
}

// Product identity is owned by this repository while the Android implementation is still
// consumed from the pinned OwnTV source tree. The namespace remains upstream-compatible;
// the installable application id is our product id.
subprojects {
    plugins.withId("com.android.application") {
        extensions.configure<ApplicationExtension> {
            defaultConfig {
                applicationId = providers.gradleProperty("product.applicationId")
                    .orElse("com.bilalmc.iptvplayer")
                    .get()
                versionName = providers.gradleProperty("product.versionName")
                    .orElse("0.1.0")
                    .get()
            }
        }
    }
}
