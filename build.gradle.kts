plugins {
    java
    kotlin("jvm") version "1.4.10"
}

group = "ru.herobrine1st.ocbridge"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.google.code.gson:gson:2.8.5")
    testCompile("junit", "junit", "4.12")
}
