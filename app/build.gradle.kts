plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
}
android {
    namespace = "com.example.pixeltoolbox"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.pixeltoolbox"
        minSdk = 28
        targetSdk = 34
        versionCode = 6
        versionName = "1.1.5"

        // ===== Call Recording (ported from ShizuCallRecorder) =====
        // scrcpy-server binary injected into assets (see app/src/main/assets/scrcpy-server)
        val scrcpyVersion = "4.0"
        val scrcpyServerSha256 = "84924bd564a1eb6089c872c7521f968058977f91f5ff02514a8c74aff3210f3a"
        val scrcpyServerAssetName = "scrcpy-server"
        buildConfigField("String", "SCRCPY_VERSION", "\"$scrcpyVersion\"")
        buildConfigField("String", "SCRCPY_SERVER_SHA256", "\"$scrcpyServerSha256\"")
        buildConfigField("String", "SCRCPY_SERVER_ASSET_NAME", "\"$scrcpyServerAssetName\"")

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("../pixeltoolbox.keystore")
            storePassword = "pixeltoolbox"
            keyAlias = "key0"
            keyPassword = "pixeltoolbox"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        aidl = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.1")
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.material:material-icons-extended")
    
    // Hilt
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-android-compiler:2.50")
    
    // Shizuku
    val shizukuVersion = "13.1.5"
    implementation("dev.rikka.shizuku:api:$shizukuVersion")
    implementation("dev.rikka.shizuku:provider:$shizukuVersion")
    
    // HiddenApiBypass for accessing hidden APIs (e.g. IActivityManager$Stub)
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")
    
    // ARSCLib for reading/writing Android binary resource files
    implementation("io.github.reandroid:ARSCLib:1.1.4")
    
    // Bouncy Castle for BKS keystore support (required by AppClone signing)
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
    
    // apksig for APK v1+v2 signing
    implementation("com.android.tools.build:apksig:8.12.1")
    
    // Glance for App Widgets
    implementation("androidx.glance:glance-appwidget:1.0.0")
    implementation("androidx.glance:glance-material3:1.0.0")
    
    // WorkManager for widget periodic updates
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // OkHttp for weather API
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    
    // ===== Call Recording (ported from ShizuCallRecorder) =====
    // SAF document files for recording output & post-call delete dialog
    implementation("androidx.documentfile:documentfile:1.0.1")
    // AppCompat for DeleteDialogConfirmationActivity (dialog theme)
    implementation("androidx.appcompat:appcompat:1.6.1")
    
    compileOnly(project(":stub"))
    implementation("androidx.hilt:hilt-navigation-compose:1.0.0")
}



