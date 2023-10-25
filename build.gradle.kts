plugins {
    kotlin("jvm") version "1.9.0"
}

group = "cz.dejfcold.keycloak"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.keycloak:keycloak-server-spi:22.0.4")
    implementation("org.keycloak:keycloak-server-spi-private:22.0.4")
    implementation("org.keycloak:keycloak-services:22.0.4")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // so far we only want Kotlin dependencies included in the jar
    from(configurations.runtimeClasspath.get()
        .filter { it.name.startsWith("kotlin-stdlib") }
        .map { if (it.isDirectory) it else zipTree(it) })
}
