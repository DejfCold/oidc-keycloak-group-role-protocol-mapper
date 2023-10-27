plugins {
    kotlin("jvm") version "1.9.0"
}

group = "cz.dejfcold.keycloak"
version = "1.0.1"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.keycloak:keycloak-server-spi:22.0.4")
    implementation("org.keycloak:keycloak-server-spi-private:22.0.5")
    implementation("org.keycloak:keycloak-services:22.0.5")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<Test> { useJUnitPlatform() }

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // so far we only want Kotlin dependencies included in the jar
    from(configurations.runtimeClasspath.get()
        .filter { it.name.startsWith("kotlin-stdlib") }
        .map { if (it.isDirectory) it else zipTree(it) })
}
