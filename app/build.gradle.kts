plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.step.tcd_rpkb"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.step.tcd_rpkb"
        minSdk = 24
        targetSdk = 30
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(libs.recyclerview)
    implementation(libs.gson)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.mediarouter)
    implementation("androidx.viewpager2:viewpager2:1.0.0")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0") // Замените на актуальную версию или libs.retrofit
    implementation("com.squareup.retrofit2:converter-gson:2.9.0") // Конвертер для Gson. Замените или используйте libs.retrofitConverterGson
    // Для логгирования HTTP запросов (опционально, но очень полезно для отладки)
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3") // Замените или используйте libs.okhttpLoggingInterceptor

    implementation(libs.hilt.android)
    annotationProcessor(libs.hilt.compiler)

    implementation("javax.inject:javax.inject:1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
