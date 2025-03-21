plugins {
  kotlin("jvm") version "2.1.10"
  kotlin("plugin.spring") version "1.9.25"
  id("org.springframework.boot") version "3.5.0-M2"
  id("io.spring.dependency-management") version "1.1.7"
}

group = "com.it1shka"
version = "0.0.1-SNAPSHOT"

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
}

repositories {
  mavenCentral()
  maven { url = uri("https://repo.spring.io/milestone") }
}

dependencies {
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
  implementation("org.springframework.boot:spring-boot-starter")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
  compilerOptions {
    freeCompilerArgs.addAll("-Xjsr305=strict")
  }
  sourceSets.all {
    // languageSettings.enableLanguageFeature("ExplicitBackingFields")
    languageSettings.enableLanguageFeature("WhenGuards")
  }
}

tasks.withType<Test> {
  useJUnitPlatform()
}
