plugins {
    id("com.android.application")
}
android {
    namespace = "com.nexus.app"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.nexus.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0-BASE"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
}
