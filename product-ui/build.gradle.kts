plugins {
    // The root build already resolves the pinned Android Gradle Plugin.
    // Use the plugin id directly here because the imported OwnTV catalog does not expose
    // an android.library accessor in the product build's generated version-catalog API.
    id("com.android.library")
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.bilalmc.iptvplayer.ui"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.paging.compose)

    // Channel logos are part of the provider data model; render them directly in the product shell.
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Keep the product pinned to the exact OwnTV_Core commit included as a submodule.
    implementation("tv.own.owntv:core:1.0.17")
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
}
