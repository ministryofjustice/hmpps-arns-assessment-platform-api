import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "10.1.0"
  id("org.jetbrains.kotlin.kapt") version "2.3.20"
  kotlin("plugin.spring") version "2.3.20"
  kotlin("plugin.jpa") version "2.3.20"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:2.1.0")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:7.2.0")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.2")
  implementation("tools.jackson.module:jackson-module-kotlin:3.1.0")
  implementation("org.springframework.retry:spring-retry")
  runtimeOnly("io.netty:netty-codec-classes-quic")

  // Database dependencies
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-data-redis")
  implementation("org.springframework.boot:spring-boot-starter-flyway")
  implementation("org.postgresql:postgresql:42.7.10")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  kapt("org.hibernate.orm:hibernate-jpamodelgen:7.3.0.Final")

  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:2.1.0")
  testImplementation("org.wiremock:wiremock-standalone:3.13.2")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.3.20")
  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.39") {
    exclude(group = "io.swagger.core.v3")
  }
  testImplementation("com.ninja-squad:springmockk:5.0.1")
}

kotlin {
  jvmToolchain(21)
}

tasks {
  withType<KotlinCompile> {
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
  }
  withType<BootRun> {
    jvmArgs = listOf(
      "-javaagent:/glowroot/glowroot.jar",
      "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005",
      "-Dglowroot.agent.id=app",
    )
  }
  withType<Test> {
    jvmArgs(
      "-javaagent:/glowroot/glowroot.jar",
      "-Dglowroot.agent.id=test"
    )
  }
}

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.compilerOptions {
  freeCompilerArgs.set(listOf("-Xannotation-default-target=param-property"))
}
