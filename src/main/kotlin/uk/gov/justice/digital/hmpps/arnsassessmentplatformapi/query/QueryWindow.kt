package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDateTime

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  JsonSubTypes.Type(value = TimeframeWindow::class, name = "TIMEFRAME"),
  JsonSubTypes.Type(value = PageWindow::class, name = "EVENTS"),
)
sealed interface Window

data class TimeframeWindow(val from: LocalDateTime, val to: LocalDateTime) : Window

data class PageWindow(val count: Int, val page: Int) : Window
