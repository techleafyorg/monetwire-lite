plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "dev.monetwire"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // api, not implementation: apps pass our callbacks SDK types (LoadAdError and friends)
    // and would otherwise have to add the same two lines themselves.
    api(libs.ads.mobile.sdk)
    api(libs.ump)
}
