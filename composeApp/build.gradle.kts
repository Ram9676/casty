import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.hilt)
    //alias(libs.plugins.conveyor)
}

repositories {
    google()
    mavenCentral()
    //mavenLocal()
    maven { url = uri("https://jitpack.io") }
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            freeCompilerArgs.add("-Xcontext-receivers")
        }
    }

    jvm("desktop")



    sourceSets {
        all {
            languageSettings {
                optIn("org.jetbrains.compose.resources.ExperimentalResourceApi")
            }
        }

        val desktopMain by getting
        desktopMain.dependencies {
            implementation(compose.components.resources)
            implementation(compose.desktop.currentOs)

            implementation(libs.material.icon.desktop)
            implementation(libs.vlcj)

            implementation(libs.coil.network.okhttp)
            runtimeOnly(libs.kotlinx.coroutines.swing)

            /*
            // Uncomment only for build jvm desktop version
            // Comment before build android version
            configurations.commonMainApi {
                exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
            }
            */





        }

        val commonMain by getting
        val androidMain by getting
        androidMain.dependencies {
            implementation(project(":innertube"))
            implementation(libs.coil.network.okhttp)
            implementation(libs.navigation)
            implementation("androidx.datastore:datastore-preferences:1.1.2")
            implementation("androidx.compose.material3:material3-window-size-class-android:${libs.versions.material3.get()}")
            implementation(libs.media3.session)
            implementation(libs.media3.ui)
            implementation(libs.kotlin.coroutines.guava)
            implementation(libs.kotlin.concurrent.futures)
            implementation(libs.crypto)
            implementation("com.github.MetrolistGroup:MetrolistExtractor:6305155") {
                exclude(group = "com.google.protobuf")
            }
            implementation(libs.nanojson)
            implementation(libs.androidx.webkit)

            // Room - best-ever persistent storage for Casty library, playlists, likes, downloads metadata
            implementation(libs.room.runtime)
            implementation(libs.room) // room-ktx (coroutines + Flow support)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(libs.mediaplayer.kmp)

            implementation(libs.navigation.kmp)

            //coil3 mp
            implementation(libs.coil.compose.core)
            implementation(libs.coil.compose)
            implementation(libs.coil.mp)

            implementation(libs.translator)
            implementation(libs.reorderable)

        }
    }
}

// Room compiler (KSP) must be declared at this level for KMP + androidMain to work reliably
dependencies {
    add("kspAndroid", libs.room.compiler.get())
}

android {
    fun Project.propertyOrEmpty(name: String): String {
        val property = findProperty(name) as String?
        return property ?: ""
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }

    buildFeatures {
        buildConfig = true
        compose = true
        resValues = true
    }

    compileSdk = 35

    defaultConfig {
        applicationId = "com.casty.music"
        minSdk = 21
        targetSdk = 35
        versionCode = 90
        versionName = "2.2026.002"

        // Aggressive size reduction for best-ever APK:
        // Only ship English + Telugu. Everything else falls back to English.
        // This removes hundreds of language resources (big win for the lyrics translation cleanup the user requested).
        resConfigs("en", "te")

        // INIT ENVIRONMENT
        resValue(
            "string",
            "env_CrQ0JjAXgv",
            propertyOrEmpty("CrQ0JjAXgv")
        )
        resValue(
            "string",
            "env_hNpBzzAn7i",
            propertyOrEmpty("hNpBzzAn7i")
        )
        resValue(
            "string",
            "env_lEi9YM74OL",
            propertyOrEmpty("lEi9YM74OL")
        )
        resValue(
            "string",
            "env_C0ZR993zmk",
            propertyOrEmpty("C0ZR993zmk")
        )
        resValue(
            "string",
            "env_w3TFBFL74Y",
            propertyOrEmpty("w3TFBFL74Y")
        )
        resValue(
            "string",
            "env_mcchaHCWyK",
            propertyOrEmpty("mcchaHCWyK")
        )
        resValue(
            "string",
            "env_L2u4JNdp7L",
            propertyOrEmpty("L2u4JNdp7L")
        )
        resValue(
            "string",
            "env_sqDlfmV4Mt",
            propertyOrEmpty("sqDlfmV4Mt")
        )
        resValue(
            "string",
            "env_WpLlatkrVv",
            propertyOrEmpty("WpLlatkrVv")
        )
        resValue(
            "string",
            "env_1zNshDpFoh",
            propertyOrEmpty("1zNshDpFoh")
        )
        resValue(
            "string",
            "env_mPVWVuCxJz",
            propertyOrEmpty("mPVWVuCxJz")
        )
        resValue(
            "string",
            "env_auDsjnylCZ",
            propertyOrEmpty("auDsjnylCZ")
        )
        resValue(
            "string",
            "env_AW52cvJIJx",
            propertyOrEmpty("AW52cvJIJx")
        )
        resValue(
            "string",
            "env_0RGAyC1Zqu",
            propertyOrEmpty("0RGAyC1Zqu")
        )
        resValue(
            "string",
            "env_4Fdmu9Jkax",
            propertyOrEmpty("4Fdmu9Jkax")
        )
        resValue(
            "string",
            "env_kuSdQLhP8I",
            propertyOrEmpty("kuSdQLhP8I")
        )
        resValue(
            "string",
            "env_QrgDKwvam1",
            propertyOrEmpty("QrgDKwvam1")
        )
        resValue(
            "string",
            "env_wLwNESpPtV",
            propertyOrEmpty("wLwNESpPtV")
        )
        resValue(
            "string",
            "env_JJUQaehRFg",
            propertyOrEmpty("JJUQaehRFg")
        )
        resValue(
            "string",
            "env_i7WX2bHV6R",
            propertyOrEmpty("i7WX2bHV6R")
        )
        resValue(
            "string",
            "env_XpiuASubrV",
            propertyOrEmpty("XpiuASubrV")
        )
        resValue(
            "string",
            "env_lOlIIVw38L",
            propertyOrEmpty("lOlIIVw38L")
        )
        resValue(
            "string",
            "env_mtcR0FhFEl",
            propertyOrEmpty("mtcR0FhFEl")
        )
        resValue(
            "string",
            "env_DTihHAFaBR",
            propertyOrEmpty("DTihHAFaBR")
        )
        resValue(
            "string",
            "env_a4AcHS8CSg",
            propertyOrEmpty("a4AcHS8CSg")
        )
        resValue(
            "string",
            "env_krdLqpYLxM",
            propertyOrEmpty("krdLqpYLxM")
        )
        resValue(
            "string",
            "env_ye6KGLZL7n",
            propertyOrEmpty("ye6KGLZL7n")
        )
        resValue(
            "string",
            "env_ec09m20YH5",
            propertyOrEmpty("ec09m20YH5")
        )
        resValue(
            "string",
            "env_LDRlbOvbF1",
            propertyOrEmpty("LDRlbOvbF1")
        )
        resValue(
            "string",
            "env_EEqX0yizf2",
            propertyOrEmpty("EEqX0yizf2")
        )
        resValue(
            "string",
            "env_i3BRhLrV1v",
            propertyOrEmpty("i3BRhLrV1v")
        )
        resValue(
            "string",
            "env_MApdyHLMyJ",
            propertyOrEmpty("MApdyHLMyJ")
        )
        resValue(
            "string",
            "env_hizI7yLjL4",
            propertyOrEmpty("hizI7yLjL4")
        )
        resValue(
            "string",
            "env_rLoZP7BF4c",
            propertyOrEmpty("rLoZP7BF4c")
        )
        resValue(
            "string",
            "env_nza34sU88C",
            propertyOrEmpty("nza34sU88C")
        )
        resValue(
            "string",
            "env_dwbUvjWUl3",
            propertyOrEmpty("dwbUvjWUl3")
        )
        resValue(
            "string",
            "env_fqqhBZd0cf",
            propertyOrEmpty("fqqhBZd0cf")
        )
        resValue(
            "string",
            "env_9sZKrkMg8p",
            propertyOrEmpty("9sZKrkMg8p")
        )
        resValue(
            "string",
            "env_aQpNCVOe2i",
            propertyOrEmpty("aQpNCVOe2i")
        )
        resValue(
            "string",
            "env_XNl2TKXLlB",
            propertyOrEmpty("XNl2TKXLlB")
        )
        resValue(
            "string",
            "env_yNjbjspY8v",
            propertyOrEmpty("yNjbjspY8v")
        )
        resValue(
            "string",
            "env_eZueG672lt",
            propertyOrEmpty("eZueG672lt")
        )
        resValue(
            "string",
            "env_WkUFhXtC3G",
            propertyOrEmpty("WkUFhXtC3G")
        )
        resValue(
            "string",
            "env_z4Xe47r8Vs",
            propertyOrEmpty("z4Xe47r8Vs")
        )
        // INIT ENVIRONMENT
    }

    splits {
        abi {
            reset()
            isUniversalApk = true
        }
    }

    namespace = "com.casty.music"

    signingConfigs {
        create("release") {
            val storePath = (findProperty("CASTY_RELEASE_STORE_FILE") as String?)
                ?: System.getenv("CASTY_RELEASE_STORE_FILE")
            val storePass = (findProperty("CASTY_RELEASE_STORE_PASSWORD") as String?)
                ?: System.getenv("CASTY_RELEASE_STORE_PASSWORD")
            val alias = (findProperty("CASTY_RELEASE_KEY_ALIAS") as String?)
                ?: System.getenv("CASTY_RELEASE_KEY_ALIAS")
            val keyPass = (findProperty("CASTY_RELEASE_KEY_PASSWORD") as String?)
                ?: System.getenv("CASTY_RELEASE_KEY_PASSWORD")

            if (!storePath.isNullOrBlank() && !storePass.isNullOrBlank() && !alias.isNullOrBlank() && !keyPass.isNullOrBlank()) {
                storeFile = file(storePath)
                storePassword = storePass
                keyAlias = alias
                keyPassword = keyPass
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            manifestPlaceholders["appName"] = "Casty-Debug"
        }

        release {
            vcsInfo.include = false
            isMinifyEnabled = true
            isShrinkResources = true
            manifestPlaceholders["appName"] = "Casty"
            val castyReleaseSigning = signingConfigs.getByName("release").takeIf {
                it.storeFile?.exists() == true &&
                    !it.storePassword.isNullOrBlank() &&
                    !it.keyAlias.isNullOrBlank() &&
                    !it.keyPassword.isNullOrBlank()
            }
            signingConfig = castyReleaseSigning ?: signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }



    flavorDimensions += "version"
    productFlavors {
        create("full") {
            dimension = "version"
        }
    }
    productFlavors {
        create("accrescent") {
            dimension = "version"
            manifestPlaceholders["appName"] = "Casty-Acc"
        }
    }

    sourceSets.all {
        kotlin.srcDir("src/$name/kotlin")
    }



    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

//    composeOptions {
//        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
//    }

    androidResources {
        generateLocaleConfig = true
    }

    // =====================================================================
    // Room KSP arguments - Casty best-ever persistent database
    // Schemas will be exported to schemas/casty/ (create dir if needed)
    // =====================================================================
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas/casty")
        arg("room.incremental", "true")
        arg("room.expandProjection", "true")
    }

}

hilt {
    enableAggregatingTask = false
}



java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

compose.desktop {
    application {

        mainClass = "MainKt"


        //conveyor
        version = "0.0.1"
        group = "casty"
/*

        nativeDistributions {
            vendor = "Casty"
            description = "Desktop music player"
        }
        */

        //jpackage
        nativeDistributions {
            //conveyor
            vendor = "Casty"
            description = "Casty Desktop Music Player"

            targetFormats(TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "Casty.DesktopApp"
            packageVersion = "0.0.1"

            /*
            val iconsRoot = project.file("desktop-icons")
            windows {
                iconFile.set(iconsRoot.resolve("icon-windows.ico"))
            }
            macOS {
                iconFile.set(iconsRoot.resolve("icon-mac.icns"))
            }
            linux {
                iconFile.set(iconsRoot.resolve("icon-linux.png"))
            }

             */
        }

    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val flavor = variant.flavorName ?: ""
            val buildType = variant.buildType ?: ""
            val baseName = if (flavor.isNotEmpty()) "${flavor}-${buildType}" else buildType
            output.outputFileName.set("casty-${baseName}.apk")
        }
    }
}

compose.resources {
    publicResClass = true
    generateResClass = always
}

dependencies {
    implementation(projects.composePersist)
    implementation(libs.compose.activity)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.util)
    implementation(libs.compose.ripple)
    implementation(libs.compose.shimmer)
    implementation(libs.compose.coil)
    implementation(libs.palette)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.datasource.okhttp)
    implementation(libs.appcompat)
    implementation(libs.appcompat.resources)
    implementation(libs.support)
    implementation(libs.media)
    implementation(libs.material)
    implementation(libs.material3)
    implementation(libs.compose.ui.graphics.android)
    implementation(libs.constraintlayout)
    implementation(libs.compose.runtime.livedata)
    implementation(libs.compose.animation)
    implementation(libs.kotlin.csv)
    implementation(libs.androidmaterial)
    implementation(libs.timber)
    implementation(libs.crypto)
    implementation(libs.logging.interceptor)
    implementation(libs.math3)
    implementation(libs.gson)

    implementation(libs.hilt)
    implementation(libs.hilt.navigation.compose)
    add("kspAndroid", libs.hilt.compiler)

    implementation(projects.kugou)
    implementation(projects.lrclib)


//    coreLibraryDesugaring(libs.desugaring)
    coreLibraryDesugaring(libs.desugaring.nio)

}
