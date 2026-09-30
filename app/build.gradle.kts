plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.googleServices)
//    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.appadslib"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.mobiapp.adslib"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("keystore/gameempire.jks")
            storePassword = "gameempire"
            keyAlias = "gameempire"
            keyPassword = "gameempire"
        }
    }


    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")

            resValue("string", "app_id", "ca-app-pub-3940256099942544~3347511713")

            resValue("string", "resume_open_app", "ca-app-pub-3940256099942544/9257395921")

            resValue("string", "banner_splash", "ca-app-pub-3940256099942544/6300978111")
            resValue("string", "inter_splash_high", "ca-app-pub-3940256099942544/1033173712")
            resValue("string", "inter_splash", "ca-app-pub-3940256099942544/1033173712")

            resValue("string", "native_language_high", "ca-app-pub-3940256099942544/1044960115")
            resValue("string", "native_language", "ca-app-pub-3940256099942544/2247696110")
            resValue("string", "native_language_high_alt", "ca-app-pub-3940256099942544/1044960115")
            resValue("string", "native_language_alt", "ca-app-pub-3940256099942544/2247696110")

            resValue("string", "native_onboarding_1", "ca-app-pub-3940256099942544/2247696110")
            resValue("string", "native_onboarding_full_1", "ca-app-pub-3940256099942544/2247696110")
            resValue("string", "native_onboarding_full_2", "ca-app-pub-3940256099942544/2247696110")
            resValue("string", "native_onboarding_3", "ca-app-pub-3940256099942544/2247696110")

            resValue("string", "native_question", "")
            resValue("string", "native_permission", "ca-app-pub-3940256099942544/2247696110")

            resValue("string", "banner_all", "ca-app-pub-3940256099942544/6300978111")
            resValue("string", "native_all", "ca-app-pub-3940256099942544/1044960115")
            resValue("string", "native_edit", "ca-app-pub-3940256099942544/2247696110")
            resValue("string", "inter_all", "ca-app-pub-3940256099942544/1033173712")


            resValue("string", "reward_all", "ca-app-pub-3940256099942544/5224354917")
            resValue("string", "banner_camera", "ca-app-pub-3940256099942544/6300978111")
        }
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            resValue("string", "app_id", "ca-app-pub-3940256099942544~3347511713")

            resValue("string", "resume_open_app", "ca-app-pub-3940256099942544/9257395921")

            resValue("string", "banner_splash", "ca-app-pub-3940256099942544/6300978111")
            resValue("string", "inter_splash_high", "ca-app-pub-3940256099942544/1033173712")
            resValue("string", "inter_splash", "ca-app-pub-3940256099942544/1033173712")

            resValue("string", "native_language_high", "ca-app-pub-3940256099942544/1044960115")
            resValue("string", "native_language", "ca-app-pub-3940256099942544/2247696110")
            resValue("string", "native_language_high_alt", "ca-app-pub-3940256099942544/1044960115")
            resValue("string", "native_language_alt", "ca-app-pub-3940256099942544/2247696110")

            resValue("string", "native_onboarding_1", "ca-app-pub-3940256099942544/2247696110")
            resValue("string", "native_onboarding_full_1", "ca-app-pub-3940256099942544/2247696110")
            resValue("string", "native_onboarding_full_2", "ca-app-pub-3940256099942544/2247696110")
            resValue("string", "native_onboarding_3", "ca-app-pub-3940256099942544/2247696110")

            resValue("string", "native_question", "")
            resValue("string", "native_permission", "ca-app-pub-3940256099942544/2247696110")

            resValue("string", "banner_all", "ca-app-pub-3940256099942544/6300978111")
            resValue("string", "native_all", "ca-app-pub-3940256099942544/1044960115")
            resValue("string", "native_edit", "ca-app-pub-3940256099942544/2247696110")
            resValue("string", "inter_all", "ca-app-pub-3940256099942544/1033173712")


            resValue("string", "reward_all", "ca-app-pub-3940256099942544/5224354917")
            resValue("string", "banner_camera", "ca-app-pub-3940256099942544/6300978111")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true // Kích hoạt tính năng Data Binding
        buildConfig = true
    }
}
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

configurations.all {
    exclude(group = "com.google.android.gms", module = "play-services-ads")
    exclude(group = "com.google.android.gms", module = "play-services-ads-lite")
    exclude(group = "com.google.android.gms", module = "play-services-ads-base")
}

dependencies {

    implementation (libs.androidx.core)
    implementation(project(":mobilibraryads"))
    implementation(libs.adjust.android)
    implementation(libs.installreferrer)
//    implementation("com.app:library-test:0.0.9")
    implementation(libs.shimmer)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}