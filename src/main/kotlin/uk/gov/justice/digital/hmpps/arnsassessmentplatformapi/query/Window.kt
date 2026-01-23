package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDateTime

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  JsonSubTypes.Type(value = Timeframe::class, name = "TIMEFRAME"),
  JsonSubTypes.Type(value = Events::class, name = "EVENTS"),
)
sealed interface Window

data class Timeframe(val from: LocalDateTime, val to: LocalDateTime) : Window

data class Events(val count: Int, val page: Int) : Window
