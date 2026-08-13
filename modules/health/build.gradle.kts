import org.gradle.api.Project

plugins {
    id("com.android.library")

}

fun Project.extraInt(name: String) = rootProject.extra[name].toString().toInt()

val compileSdkValue = extraInt("compileSdk")
val minSdkValue = extraInt("minSdk")

android {
    namespace = "com.zero.health"
    compileSdk = compileSdkValue
    ndkVersion = "28.2.13676358"

    defaultConfig {
        minSdk = minSdkValue
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        externalNativeBuild {
            cmake {
                cppFlags += ""
            }
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.18.1"
        }
    }
}

dependencies {
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    implementation(project(":modules:base"))
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)
}
