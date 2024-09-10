plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.mrincredible.aistudio"
    compileSdk = 34

    packaging {
//        dex {
//            useLegacyPackaging = false
//        }
        resources.excludes.add("META-INF/*")
    }

    defaultConfig {
        applicationId = "com.mrincredible.aistudio"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.text.recognition)
    implementation(libs.generativeai)
    implementation(libs.guava)
    implementation(libs.reactive.streams)
    implementation("com.vladsch.flexmark:flexmark-all:0.64.8")
    implementation ("org.commonmark:commonmark:0.22.0")
//    implementation ("com.google.android.material:material:1.6.1")
}