package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.GetAssessmentsSoftDeletedSinceQuery

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  JsonSubTypes.Type(value = TimelineQueryResult::class, name = "TimelineQueryResult"),
  JsonSubTypes.Type(value = AssessmentVersionQueryResult::class, name = "AssessmentVersionQueryResult"),
  JsonSubTypes.Type(value = CollectionItemQueryResult::class, name = "CollectionItemQueryResult"),
  JsonSubTypes.Type(value = CollectionQueryResult::class, name = "CollectionQueryResult"),
  JsonSubTypes.Type(value = DailyVersionsQueryResult::class, name = "DailyVersionsQueryResult"),
  JsonSubTypes.Type(value = GetAssessmentsModifiedSinceQueryResult::class, name = "GetAssessmentsModifiedSinceQueryResult"),
  JsonSubTypes.Type(value = GetAssessmentsSoftDeletedSinceQueryResult::class, name = "GetAssessmentsSoftDeletedSinceQueryResult"),
)
sealed interface QueryResult
