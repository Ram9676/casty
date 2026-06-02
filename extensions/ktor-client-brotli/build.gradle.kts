plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(21)
}

sourceSets.all {
    java.srcDir("src/$name/kotlin")
}

dependencies {
    implementation(libs.ktor.client.encoding)
    implementation(libs.brotli)
}