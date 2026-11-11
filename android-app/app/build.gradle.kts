plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.chaquo.python")
}

val sourceCommit = providers.gradleProperty("SOURCE_COMMIT").orElse("dev").get()
val chaquopyPythonVersion = providers.gradleProperty("CHAQUOPY_PYTHON_VERSION").orElse("3.12").get()

android {
    namespace = "com.erawat.arc"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.erawat.arc"
        minSdk = 26
        targetSdk = 35
        val vCode = providers.gradleProperty("VERSION_CODE").orElse("1").get().toInt()
        versionCode = vCode
        versionName = providers.gradleProperty("VERSION_NAME").orElse("0.1.0-local").get()
        val buildNumber = providers.gradleProperty("BUILD_NUMBER").orElse(vCode.toString()).get().toInt()

        // CI can replace this value at build time with the commit used to build the APK.
        buildConfigField("String", "SOURCE_COMMIT", "\"$sourceCommit\"")
        buildConfigField("int", "BUILD_NUMBER", "$buildNumber")
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

chaquopy {
    defaultConfig {
        version = chaquopyPythonVersion
        pip {
            install("requests")
            install("beautifulsoup4")
            install("yt-dlp")
            // Pyrogram depends on pyaes==1.6.1, which Chaquopy's Python 3.14
            // index does not publish. Bundle the pure-Python package locally.
            install("../python-vendor/pyaes")
            install("pyrogram==2.0.106")
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.5.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
