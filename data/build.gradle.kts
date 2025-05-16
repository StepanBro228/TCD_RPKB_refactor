plugins {
    id("com.android.library")
    // Если вы планируете использовать Kotlin в data модуле, раскомментируйте следующую строку
    // id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.step.tcd_rpkb.data"
    compileSdk = 35 // Рекомендуется использовать ту же версию, что и в app модуле, или актуальную стабильную

    defaultConfig {
        minSdk = 24
        // targetSdk для библиотечных модулей обычно не указывается явно,
        // но если требуется, должно соответствовать targetSdk приложения.

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    // Если используете Kotlin, добавьте:
    // kotlinOptions {
    //    jvmTarget = "11"
    // }
}

dependencies {
    implementation(project(":domain"))

    implementation(libs.gson) // Уже есть
    // implementation("com.google.code.gson:gson:YOUR_GSON_VERSION") // Если libs.gson не определен

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0") // Замените на актуальную версию или libs.retrofit
    implementation("com.squareup.retrofit2:converter-gson:2.9.0") // Конвертер для Gson. Замените или используйте libs.retrofitConverterGson
    // Для логгирования HTTP запросов (опционально, но очень полезно для отладки)
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3") // Замените или используйте libs.okhttpLoggingInterceptor

    // Зависимости для работы с данными (примеры):
    // implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // implementation("androidx.room:room-runtime:2.5.0")
    // annotationProcessor("androidx.room:room-compiler:2.5.0")
    // Если используете Kotlin и корутины с Room:
    // implementation("androidx.room:room-ktx:2.5.0")

    implementation(libs.hilt.android)
    annotationProcessor(libs.hilt.compiler)

    implementation("javax.inject:javax.inject:1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
} 