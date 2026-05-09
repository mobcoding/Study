import org.gradle.api.Project

plugins {
    id("com.android.library")
}

fun Project.extraInt(name: String) = rootProject.extra[name].toString().toInt()

val compileSdkValue = extraInt("compileSdk")
val minSdkValue = extraInt("minSdk")

android {
    namespace = "com.toolkit.guide"
    compileSdk = compileSdkValue

    defaultConfig {
        minSdk = minSdkValue
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(libs.appcompat)
}
