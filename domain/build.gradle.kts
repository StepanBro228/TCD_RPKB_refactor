plugins {
    id("java-library")

}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {

    implementation("javax.inject:javax.inject:1")

} 