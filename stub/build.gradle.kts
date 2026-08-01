plugins {
    id("com.android.library")
}

android {
    namespace = "com.example.stub"
    compileSdk = 34
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        aidl = true
    }
}
