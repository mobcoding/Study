import org.gradle.api.Project

plugins {
    id("com.android.library")
}

fun Project.extraInt(name: String) = rootProject.extra[name].toString().toInt()

val compileSdkValue = extraInt("compileSdk")
val minSdkValue = extraInt("minSdk")

android {
    namespace = "com.zero.library_base"
    compileSdk = compileSdkValue

    defaultConfig {
        minSdk = minSdkValue
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    api(libs.androidx.core.ktx)
    api(libs.appcompat)
    api(libs.material)
    api(libs.swiperefresh.layout)
    api(libs.androidx.lifecycle.process)
    api(libs.androidx.lifecycle.viewmodel.ktx)
    api(libs.converter.gson)
    api(libs.gson)
    api(libs.glide)
    api(libs.mmkv)
    api(libs.bundles.room)
    api(libs.lottie)
    api(libs.flexbox)
    api(libs.androidx.activity.ktx)
    api(libs.androidx.fragment.ktx)
    api(project(":modules:net"))
}
