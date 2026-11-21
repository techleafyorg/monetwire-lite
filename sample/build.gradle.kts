import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Your own phones' test IDs live in local.properties, which never gets committed:
//   monetwire.testDevices=5E11D366F565C9053F7E175CCF26913C,ANOTHERONE
// Without one, a real phone gets live ads and ignores the debug geography.
val testDevices = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.reader()?.use(::load)
}.getProperty("monetwire.testDevices", "")

android {
    namespace = "dev.monetwire.sample"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "dev.monetwire.sample"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "TEST_DEVICES", "\"$testDevices\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":monetwire"))
}
