import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "io.github.andrearandazzo.papyrus"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.andrearandazzo.papyrus"
        minSdk = 26
        targetSdk = 36
        versionCode = 7
        versionName = "1.5.0"
    }

    // The release key lives outside the project (never in git). Without it the release build is
    // unsigned, which is what F-Droid expects: it builds from source and signs with its own key.
    val keystoreProperties = File(System.getProperty("user.home"), ".papyrus/keystore.properties")
    signingConfigs {
        if (keystoreProperties.exists()) {
            create("release") {
                val props = Properties().apply { keystoreProperties.inputStream().use(::load) }
                storeFile = File(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Lets a test build sit next to the real app on the same phone.
            applicationIdSuffix = ".debug"
            // Text recognition is native code: phones, plus PC emulators (x86_64) for testing.
            ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("release")
            // 64-bit phones only (every phone of the last years): the recognition engine is native
            // code, and each extra processor type would add about 3 MB. Not for x86 Chromebooks.
            ndk { abiFilters += listOf("arm64-v8a") }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Native code compressed in the APK: about half the download, unpacked once at install.
    packaging { jniLibs { useLegacyPackaging = true } }

    buildFeatures {
        compose = true
    }

    // Don't embed the encrypted, Google-only dependency report in the APK: F-Droid rejects it,
    // and it would make F-Droid's build differ from ours (reproducible builds).
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    // Lists the translated languages (res/values-*) so Android 13+ offers them in
    // Settings → Apps → Papyrus → Language. New translations are picked up automatically.
    androidResources {
        generateLocaleConfig = true
        // Ship only the languages Papyrus is translated into, so library texts (e.g. the date
        // picker) never appear in a third language. Add new translations here too.
        localeFilters += listOf("en", "it")
    }
}

// Room writes the database schema here so future versions can be migrated safely.
room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    debugImplementation(libs.androidx.compose.ui.tooling)
    // Text recognition (Tesseract, Apache-2.0), only on JitPack; see settings.gradle.kts.
    implementation(libs.tesseract4android)
    testImplementation(libs.junit)
    testImplementation(libs.org.json)
}
