package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDateTime

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  JsonSubTypes.Type(value = TimelineQuery::class, name = "TimelineQuery"),
  JsonSubTypes.Type(value = AssessmentVersionQuery::class, name = "AssessmentVersionQuery"),
  JsonSubTypes.Type(value = CollectionItemQuery::class, name = "CollectionItemQuery"),
  JsonSubTypes.Type(value = CollectionQuery::class, name = "CollectionQuery"),
  JsonSubTypes.Type(value = DailyVersionsQuery::class, name = "DailyVersionsQuery"),
)
sealed interface Query {
  val timestamp: LocalDateTime?
}
