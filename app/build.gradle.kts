import com.github.megatronking.stringfog.plugin.StringFogMode
import com.github.megatronking.stringfog.plugin.kg.RandomKeyGenerator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    id("com.google.devtools.ksp")
}

fun Project.extraInt(name: String) = (rootProject.extra[name] as Number).toInt()
fun Project.extraString(name: String) = rootProject.extra[name].toString()

val compileSdkVersion = extraInt("compileSdk")
val minSdkVersion = extraInt("minSdk")
val targetSdkVersion = extraInt("targetSdk")
val applicationIdValue = extraString("APPLICATION_ID")
val storePath = extraString("STORE_PATH")
val storePasswordValue = extraString("STORE_PASSWORD")
val keyAliasValue = extraString("KEY_ALIAS")
val keyPasswordValue = extraString("KEY_PASSWORD")

val appVersionName = "1.1"

apply(plugin = "com.bytedance.android.aabResGuard")
apply(plugin = "stringfog")

android {
    namespace = "com.zero.study"
    compileSdk = compileSdkVersion

    defaultConfig {
        applicationId = applicationIdValue
        minSdk = minSdkVersion
        targetSdk = targetSdkVersion
        versionCode = 1
        versionName = appVersionName
        buildConfigField("String", "COUNTRY_API_URL", "\"https://ipinfo.io/json/\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file(storePath)
            storePassword = storePasswordValue
            keyAlias = keyAliasValue
            keyPassword = keyPasswordValue
            enableV2Signing = true
        }

        create("release") {
            storeFile = file(storePath)
            storePassword = storePasswordValue
            keyAlias = keyAliasValue
            keyPassword = keyPasswordValue
            enableV2Signing = true
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("release")
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        aidl = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

extensions.configure<Any>("stringfog") {
    withGroovyBuilder {
        setProperty("implementation", "com.github.megatronking.stringfog.xor.StringFogImpl")
        setProperty("packageName", "com.zero.study")
        setProperty("enable", true)
        setProperty("fogPackages", listOf("com.zero.study"))
        setProperty("kg", RandomKeyGenerator())
        setProperty("mode", StringFogMode.bytes)
    }
}

androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        variant.outputs.forEach { output ->
            val createTime = SimpleDateFormat("yyyyMMddHHmm", Locale.ROOT).apply {
                timeZone = TimeZone.getTimeZone("GMT+08:00")
            }.format(Date())
            val fileName = "Study_${variant.buildType}_V${appVersionName}_${createTime}.apk"
            runCatching {
                @Suppress("UNCHECKED_CAST") val outputFileName = output.javaClass.getMethod("getOutputFileName").invoke(output) as Property<String>
                outputFileName.set(fileName)
            }
        }
    }
}

extensions.configure<Any>("aabResGuard") {
    withGroovyBuilder {
        setProperty("mappingFile", file("mapping.txt").toPath())
        setProperty("whiteList",
            listOf("*.R.raw.*", "*.R.drawable.icon", "*.R.string.default_web_client_id", "*.R.string.firebase_database_url", "*.R.string.gcm_defaultSenderId", "*.R.string.google_api_key",
                "*.R.string.google_app_id", "*.R.string.google_crash_reporting_api_key", "*.R.string.google_storage_bucket", "*.R.string.project_id", "*.R.string.com.crashlytics.android.build_id"))
        setProperty("obfuscatedBundleFileName", "app_build.aab")
        setProperty("mergeDuplicatedRes", true)
        setProperty("enableFilterFiles", true)
        setProperty("filterList", listOf("BUNDLE-METADATA/*"))
        setProperty("enableFilterStrings", false)
        setProperty("unusedStringPath", file("unused.txt").toPath())
        setProperty("languageWhiteList", listOf("en", "ar", "de", "es", "fr", "hi", "in", "ja", "ko", "pt", "ru"))
    }
}

dependencies {
    implementation(project(":modules:admob"))
    implementation(project(":modules:base"))
    implementation(project(":modules:health"))
    implementation(project(":modules:retention"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    implementation(libs.xor)
    implementation(libs.okhttp.profiler)
    ksp(libs.androidx.room.compiler)
    compileOnly(libs.xposed)
    implementation(libs.coil)
    implementation(libs.onesignal)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)
}
