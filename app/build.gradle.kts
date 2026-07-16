plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.youtube.musica"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.youtube.musica"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation("androidx.activity:activity-ktx:1.13.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.14.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.0")
    implementation("androidx.navigation:navigation-fragment:2.9.8")
    implementation("androidx.navigation:navigation-ui:2.9.8")
    implementation("androidx.activity:activity:1.13.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")

    implementation("com.google.firebase:firebase-firestore:26.4.1")
    implementation("com.google.firebase:firebase-auth:24.2.0")
    implementation("com.google.firebase:firebase-crashlytics:19.0.1")
    implementation("com.google.android.gms:play-services-auth:21.6.0")
    implementation("com.pierfrancescosoffritti.androidyoutubeplayer:core:13.0.0")
    implementation("com.pierfrancescosoffritti.androidyoutubeplayer:chromecast-sender:0.32")
    implementation("com.github.bumptech.glide:glide:4.16.0")

    //implementation("com.pierfrancescosoffritti.androidyoutubeplayer:chromecast-sender:0.23")
    implementation("androidx.mediarouter:mediarouter:1.8.1")
    implementation("androidx.media:media:1.7.0")
}