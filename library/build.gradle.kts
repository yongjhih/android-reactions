import com.android.build.gradle.LibraryExtension

plugins {
    id("com.android.library")
    kotlin("android")
}

configure<LibraryExtension> {
    compileSdkVersion(Config.targetSdk)

    defaultConfig {
        minSdkVersion(Config.minSdk)
        targetSdkVersion(Config.targetSdk)
    }

    resourcePrefix("reactions_")

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    api(Kotlin.stdlib.jdk7)
    api(AndroidX.core)
}

apply {
    from("publish.gradle")
}