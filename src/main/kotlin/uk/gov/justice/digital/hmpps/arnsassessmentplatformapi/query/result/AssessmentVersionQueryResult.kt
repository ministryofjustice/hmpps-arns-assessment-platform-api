package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.Answers
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.Collaborators
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.Collections
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.FormVersion
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.Properties
import java.time.LocalDateTime
import java.util.UUID

data class AssessmentVersionQueryResult(
  val assessmentUuid: UUID,
  val aggregateUuid: UUID,
  val formVersion: FormVersion,
  val createdAt: LocalDateTime,
  val updatedAt: LocalDateTime,
  @Schema(ref = "#/components/schemas/Answers")
  val answers: Answers,
  val properties: Properties,
  val collections: Collections,
  val collaborators: Collaborators,
) : QueryResult
