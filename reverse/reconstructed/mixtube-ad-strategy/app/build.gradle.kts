plugins {
    id("com.android.application")
}

android {
    namespace = "com.rebuild.mixtube.ads"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.rebuild.mixtube.ads"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
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
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("com.google.code.gson:gson:2.14.0")
    implementation("com.google.android.gms:play-services-ads:25.2.0")
}
