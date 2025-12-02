package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  JsonSubTypes.Type(value = AssessmentTimelineQueryResult::class, name = "AssessmentTimelineQueryResult"),
  JsonSubTypes.Type(value = AssessmentVersionQueryResult::class, name = "AssessmentVersionQueryResult"),
  JsonSubTypes.Type(value = CollectionItemQueryResult::class, name = "CollectionItemQueryResult"),
  JsonSubTypes.Type(value = CollectionQueryResult::class, name = "CollectionQueryResult"),
)
sealed interface QueryResult
