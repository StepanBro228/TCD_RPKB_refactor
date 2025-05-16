plugins {
    id("java-library")
    // Если вы планируете использовать Kotlin в domain модуле, раскомментируйте следующую строку
    // id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    // Javax Inject
    implementation("javax.inject:javax.inject:1")

    // Зависимости для тестирования (опционально)
    // testImplementation("junit:junit:4.13.2")
    // testImplementation("org.mockito:mockito-core:3.12.4")
} 