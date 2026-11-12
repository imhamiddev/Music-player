plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.imhamiddev.musicplayer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.imhamiddev.musicplayer"
        minSdk = 26
        targetSdk = 35
        versionCode = (project.findProperty("appCode") as String?)?.toInt() ?: 1
        versionName = (project.findProperty("appVersion") as String?) ?: "1.0.0"
    }

    // Release key comes from CI secrets (KEYSTORE_PASSWORD / KEY_ALIAS, file restored to app/release.jks).
    // Without them (local builds) it falls back to the debug key so the project still builds.
    val ksPass = System.getenv("KEYSTORE_PASSWORD")
    val ksAlias = System.getenv("KEY_ALIAS")
    val hasReleaseKey = !ksPass.isNullOrEmpty() && !ksAlias.isNullOrEmpty() && file("release.jks").exists()
    signingConfigs {
        if (hasReleaseKey) {
            create("release") {
                storeFile = file("release.jks")
                storePassword = ksPass
                keyAlias = ksAlias
                keyPassword = ksPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName(if (hasReleaseKey) "release" else "debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-session:1.4.1")
    implementation("com.google.guava:guava:33.3.1-android")
}
