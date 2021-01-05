plugins {
    java
    kotlin("jvm") version "1.4.10"
}

group = "ru.herobrine1st.ocbridge"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.google.code.gson:gson:2.8.5")
    implementation("org.slf4j:slf4j-api:1.7.25")
    testImplementation("junit", "junit", "4.12")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}