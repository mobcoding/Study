import org.gradle.api.Project

plugins {
    id("com.android.library")

}

fun Project.extraInt(name: String) = rootProject.extra[name].toString().toInt()

val compileSdkValue = extraInt("compileSdk")
val minSdkValue = extraInt("minSdk")

android {
    namespace = "com.drake.net"
    compileSdk = compileSdkValue

    defaultConfig {
        minSdk = minSdkValue
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.androidx.documentfile)
    implementation(libs.okhttp)
}
