plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.casty.music.backend.innertube"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.client.encoding)
    implementation(libs.brotli)
    implementation("com.github.MetrolistGroup:MetrolistExtractor:6305155") {
        exclude(group = "com.google.protobuf")
    }
    implementation(libs.timber)

    coreLibraryDesugaring(libs.desugaring.nio)
}
