plugins {
    alias(libs.plugins.android.application)
}

import java.util.Properties

// Optional overrides in local.properties (gitignored, per-machine):
//   apiUrlDebug=https://staging.example.com/api/
//   apiUrlRelease=https://api.example.com/api/
//   googleWebClientId=YOUR_WEB_OAUTH_CLIENT_ID.apps.googleusercontent.com
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
val googleWebClientId: String = localProps.getProperty("googleWebClientId")
    ?: System.getenv("HAFIZ_GOOGLE_WEB_CLIENT_ID")
    ?: ""

val releaseKeystorePath = System.getenv("HAFIZ_KEYSTORE_PATH")
    ?: localProps.getProperty("releaseKeystorePath")
val releaseStorePassword = System.getenv("HAFIZ_KEYSTORE_PASSWORD")
    ?: localProps.getProperty("releaseKeystorePassword")
val releaseKeyAlias = System.getenv("HAFIZ_KEY_ALIAS")
    ?: localProps.getProperty("releaseKeyAlias")
val releaseKeyPassword = System.getenv("HAFIZ_KEY_PASSWORD")
    ?: localProps.getProperty("releaseKeyPassword")
val releaseSigningReady = !releaseKeystorePath.isNullOrBlank()
    && rootProject.file(releaseKeystorePath).isFile
    && !releaseStorePassword.isNullOrBlank()
    && !releaseKeyAlias.isNullOrBlank()
    && !releaseKeyPassword.isNullOrBlank()

android {
    namespace = "com.hafiztraveltours.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.hafiztraveltours.app"
        minSdk = 24
        targetSdk = 37
        versionCode = 6
        versionName = "1.6"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
    }

    signingConfigs {
        // Release signing comes ONLY from environment/CI secrets or untracked
        // local.properties keys (releaseKeystorePath/Password/Alias/KeyPassword
        // or HAFIZ_KEYSTORE_PATH/PASSWORD, HAFIZ_KEY_ALIAS, HAFIZ_KEY_PASSWORD).
        // Nothing secret is committed. Release builds fail unless signing is complete.
        create("release") {
            if (releaseSigningReady) {
                storeFile = rootProject.file(releaseKeystorePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            resValue("string", "app_name", "Hafiz Travel")
            buildConfigField("String", "API_BASE_URL", "\"$debugApiUrl\"")
            manifestPlaceholders["cleartextTraffic"] = true
        }
        create("staging") {
            initWith(getByName("debug"))
            // Staging talks to the production API: never debuggable.
            isDebuggable = false
            buildConfigField("String", "API_BASE_URL", "\"$releaseApiUrl\"")
            manifestPlaceholders["cleartextTraffic"] = false
        }
        release {
            resValue("string", "app_name", "Hafiz Travel")
            buildConfigField("String", "API_BASE_URL", "\"$releaseApiUrl\"")
            manifestPlaceholders["cleartextTraffic"] = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (releaseSigningReady) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    buildFeatures {
        buildConfig = true
        resValues = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Google Identity Credential Manager. No Firebase authentication.
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")

    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.core:core-splashscreen:1.0.1")
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

tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    doFirst {
        check(releaseSigningReady) {
            "Release signing is required. Set HAFIZ_KEYSTORE_PATH, HAFIZ_KEYSTORE_PASSWORD, " +
                "HAFIZ_KEY_ALIAS, and HAFIZ_KEY_PASSWORD through environment variables or ignored local.properties."
        }
        check(googleWebClientId.isNotBlank()) {
            "Google Sign-In requires HAFIZ_GOOGLE_WEB_CLIENT_ID or googleWebClientId in ignored local.properties."
        }
    }
}
