import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "11.0.6"
  id("org.jetbrains.kotlin.kapt") version "2.4.10"
  kotlin("plugin.spring") version "2.4.10"
  kotlin("plugin.jpa") version "2.4.10"
}

sourceSets {
  create("integrationTest") {
    compileClasspath += sourceSets["main"].output + sourceSets["test"].output
    runtimeClasspath += sourceSets["main"].output + sourceSets["test"].output
  }
}

configurations.named("integrationTestImplementation") {
  extendsFrom(configurations.testImplementation.get())
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

ext["netty.version"] = "4.2.17.Final"
ext["httpclient5.version"] = "5.6.4"
ext["httpcore5.version"] = "5.4.3"
ext["tomcat.version"] = "11.0.25"

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:3.0.1")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:7.4.1")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")
  implementation("org.webjars:swagger-ui:5.32.14")
  implementation("tools.jackson.module:jackson-module-kotlin:3.2.1")
  implementation("org.springframework.retry:spring-retry")
  runtimeOnly("io.netty:netty-codec-classes-quic")

  // Database dependencies
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-data-redis")
  implementation("org.springframework.boot:spring-boot-starter-flyway")
  implementation("org.postgresql:postgresql:42.7.13")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  kapt("org.hibernate.orm:hibernate-jpamodelgen:7.4.5.Final")

  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:3.0.1")
  testImplementation("org.wiremock:wiremock-standalone:3.13.2")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.4.10")
  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.45") {
    exclude(group = "io.swagger.core.v3")
  }
  testImplementation("com.ninja-squad:springmockk:5.0.1")
}

kotlin {
  jvmToolchain(25)
}

tasks {
  withType<KotlinCompile> {
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25
  }
  withType<BootRun> {
    jvmArgs = listOf(
      "-javaagent:/glowroot/glowroot.jar",
      "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005",
      "-Dglowroot.agent.id=app",
    )
  }
  withType<Test> {
    if (File("/glowroot/glowroot.jar").exists()) {
      jvmArgs(
        "-javaagent:/glowroot/glowroot.jar",
        "-Dglowroot.agent.id=test",
      )
    }
  }
}

tasks.test {
  exclude("**/src/integrationTest/**")
}

tasks.register<Test>("integrationTest") {
  description = "Runs integration tests."
  group = "verification"

  testClassesDirs = sourceSets["integrationTest"].output.classesDirs
  classpath = sourceSets["integrationTest"].runtimeClasspath

  // Optional: Force tests to run even if outputs haven't changed
  outputs.upToDateWhen { false }

  useJUnitPlatform()
}

tasks.named("integrationTest") {
  onlyIf {
    !gradle.startParameter.taskNames.any { it.contains("koverHtmlReport") }
  }
}

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.compilerOptions {
  freeCompilerArgs.set(listOf("-Xannotation-default-target=param-property"))
}
