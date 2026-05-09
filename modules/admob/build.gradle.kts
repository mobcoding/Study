import org.gradle.api.Project

plugins {
    id("com.android.library")
}

fun Project.extraInt(name: String) = rootProject.extra[name].toString().toInt()
fun Project.extraString(name: String) = rootProject.extra[name].toString()
fun quoteValue(value: String) = "\"$value\""

val compileSdkValue = extraInt("compileSdk")
val minSdkValue = extraInt("minSdk")
val manifestAdmobAppId = extraString("MANIFEST_ADMOB_APP_ID")
val admobOpen = extraString("ADMOB_OPEN")
val admobInterstitialGuide = extraString("ADMOB_INTERSTITIAL_GUIDE")
val admobInterstitialLanguage = extraString("ADMOB_INTERSTITIAL_LANGUAGE")
val admobInterstitialConnectResult = extraString("ADMOB_INTERSTITIAL_CONNECT_RESULT")
val nativeBannerHome = extraString("NATIVE_BANNER_HOME")
val nativeBannerLanguage = extraString("NATIVE_BANNER_LANGUAGE")
val nativeBannerEditor = extraString("NATIVE_BANNER_EDITOR")

android {
    namespace = "com.toolkit.admob_libray"
    compileSdk = compileSdkValue

    defaultConfig {
        minSdk = minSdkValue
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        manifestPlaceholders["MANIFEST_ADMOB_APP_ID"] = manifestAdmobAppId
        buildConfigField("String", "ADMOB_OPEN", quoteValue(admobOpen))
        buildConfigField("String", "ADMOB_INTERSTITIAL_GUIDE", quoteValue(admobInterstitialGuide))
        buildConfigField("String", "ADMOB_INTERSTITIAL_LANGUAGE", quoteValue(admobInterstitialLanguage))
        buildConfigField(
            "String",
            "ADMOB_INTERSTITIAL_CONNECT_RESULT",
            quoteValue(admobInterstitialConnectResult)
        )
        buildConfigField("String", "NATIVE_BANNER_HOME", quoteValue(nativeBannerHome))
        buildConfigField("String", "NATIVE_BANNER_LANGUAGE", quoteValue(nativeBannerLanguage))
        buildConfigField("String", "NATIVE_BANNER_EDITOR", quoteValue(nativeBannerEditor))
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
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
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    implementation(project(":modules:base"))
    api(platform(libs.firebase.bom))
    api(libs.firebase.analytics)
    api(libs.play.services.ads)
    api(libs.firebase.config)
}
