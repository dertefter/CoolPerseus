plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "org.lineageos.settings.device"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "org.lineageos.settings.device"
        minSdk = 29
        targetSdk = 37
        versionCode = 38
        versionName = "modified my CoolPerseus"
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("debug")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
}