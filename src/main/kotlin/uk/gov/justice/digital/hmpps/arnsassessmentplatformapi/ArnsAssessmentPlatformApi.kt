package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class ArnsAssessmentPlatformApi

fun main(args: Array<String>) {
  runApplication<ArnsAssessmentPlatformApi>(*args)
}
