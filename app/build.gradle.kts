plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services") version "4.4.2"
}

import java.util.Properties

// Optional overrides in local.properties (gitignored, per-machine):
//   apiUrlDebug=https://staging.example.com/api/
//   apiUrlRelease=https://api.example.com/api/
val localProps = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) {
    localPropsFile.inputStream().use { localProps.load(it) }
}
val debugApiUrl: String =
    (localProps.getProperty("apiUrlDebug") ?: System.getenv("HAFIZ_API_URL_DEBUG")
        ?: "http://127.0.0.1:8000/api/")
val releaseApiUrl: String =
    (localProps.getProperty("apiUrlRelease") ?: System.getenv("HAFIZ_API_URL_RELEASE")
        ?: "https://api.hafiztraveltours.com/api/")
require(!releaseApiUrl.contains("127.0.0.1") && !releaseApiUrl.contains("10.0.2.2")
        && releaseApiUrl.startsWith("https://")) {
    "Release API URL must be HTTPS and not a local address. Set apiUrlRelease in local.properties."
}

android {
    namespace = "com.hafiztraveltours.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.hafiztraveltours.app"
        minSdk = 24
        targetSdk = 37
        versionCode = 4
        versionName = "1.4"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"$debugApiUrl\"")
            manifestPlaceholders["cleartextTraffic"] = true
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"$releaseApiUrl\"")
            manifestPlaceholders["cleartextTraffic"] = false
            optimization {
                enable = false
            }
        }
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Firebase BoM & Auth
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    // Google Sign-In SDK
    implementation(libs.play.services.auth)
    implementation(libs.constraintlayout)

    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation("com.batoulapps.adhan:adhan:1.2.1")
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Retrofit & Network
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Architecture (H1): ViewModel + LiveData for Profile (pinned to transitive 2.6.1, offline-cached)
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.6.1")
    implementation("androidx.lifecycle:lifecycle-livedata-core:2.6.1")

    // Skeleton UI Shimmer
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
}