plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.dertefter.coolperseus.data"
    compileSdk { version = release(37) { minorApiLevel = 2 } }

    defaultConfig {
        minSdk = 34
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

}

dependencies {
    api(libs.hilt.android)
    ksp(libs.hilt.compiler)
    runtimeOnly(libs.androidx.transition)
    api(libs.dagger)
    implementation(libs.hilt.core)
    api(libs.javax.inject)
    api(libs.kotlinx.coroutines.core)
}