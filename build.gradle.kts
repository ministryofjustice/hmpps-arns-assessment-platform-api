import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "11.0.11"
  id("org.jetbrains.kotlin.kapt") version "2.4.20"
  kotlin("plugin.spring") version "2.4.20"
  kotlin("plugin.jpa") version "2.4.20"
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

version = "0.0.1-test"

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

ext["netty.version"] = "4.2.18.Final"
ext["httpclient5.version"] = "5.6.4"
ext["httpcore5.version"] = "5.4.3"
ext["tomcat.version"] = "11.0.26"

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:3.0.3")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:7.4.1")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1")
  implementation("org.webjars:swagger-ui:5.33.1")
  implementation("tools.jackson.module:jackson-module-kotlin:3.2.3")
  implementation("org.springframework.retry:spring-retry")
  runtimeOnly("io.netty:netty-codec-classes-quic")

  // Database dependencies
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-data-redis")
  implementation("org.springframework.boot:spring-boot-starter-flyway")
  implementation("org.postgresql:postgresql:42.7.13")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  kapt("org.hibernate.orm:hibernate-jpamodelgen:7.4.12.Final")

  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:3.0.3")
  testImplementation("org.wiremock:wiremock-standalone:3.13.2")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.4.20")
  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.48") {
    exclude(group = "io.swagger.core.v3")
  }
  testImplementation("com.ninja-squad:springmockk:5.0.1")
  testImplementation("au.com.dius.pact.provider:junit5spring:4.7.5")
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
  exclude("*PactTest")
}

tasks.register<Test>("integrationTest") {
  description = "Runs integration tests."
  group = "verification"

  testClassesDirs = sourceSets["integrationTest"].output.classesDirs
  classpath = sourceSets["integrationTest"].runtimeClasspath

  // Optional: Force tests to run even if outputs haven't changed
  outputs.upToDateWhen { false }

  useJUnitPlatform()

  testLogging {
    showStandardStreams = true
    events("passed", "skipped", "failed")

    showExceptions = true
    showCauses = true
    showStackTraces = true
  }
}

tasks.register<Test>("pactTest") {
  description = "Run and publish Pact provider tests"
  group = "verification"

  val testSourceSet = sourceSets["test"]

  testClassesDirs = testSourceSet.output.classesDirs
  classpath = testSourceSet.runtimeClasspath

  filter {
      includeTestsMatching("*PactTest")
  }

  systemProperty("pactbroker.url", System.getenv("PACT_BROKER_URL"))
  systemProperty("pactbroker.auth.username", System.getenv("PACT_BROKER_USERNAME") ?: "")
  systemProperty("pactbroker.auth.password", System.getenv("PACT_BROKER_PASSWORD") ?: "")

  val consumerBranch = System.getenv("PACT_CONSUMER_BRANCH") ?: "consumer-contract-test-load-plan"
  val requestedConsumerName = System.getenv("PACT_CONSUMER_NAME") ?: "hmpps-arns-assessment-platform-ui"

  systemProperty("pactbroker.providerBranch", System.getenv("GITHUB_BRANCH") ?: "local")
  systemProperty("pact.verifier.publishResults", System.getenv("PACT_PUBLISH_RESULTS") ?: "false")
  systemProperty("pact.provider.version", System.getenv("PACT_PROVIDER_APP_VERSION") ?: "local")
  systemProperty("pact.provider.branch", System.getenv("GITHUB_BRANCH") ?: "local")
}

tasks.named("integrationTest") {
  onlyIf {
    !gradle.startParameter.taskNames.any { it.contains("koverHtmlReport") }
  }
}
