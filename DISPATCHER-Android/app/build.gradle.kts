plugins {
    id("com.android.application")
}

android {
    namespace = "gr.koukamedics.dispatcher"
    compileSdk = 36

    defaultConfig {
        applicationId = "gr.koukamedics.dispatcher"
        minSdk = 24
        targetSdk = 36
        versionCode = 3
        versionName = "1.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = false
    }

    testOptions {
        unitTests.isIncludeAndroidResources = false
    }
}

dependencies {
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.browser:browser:1.8.0")
    testImplementation("junit:junit:4.13.2")
}
