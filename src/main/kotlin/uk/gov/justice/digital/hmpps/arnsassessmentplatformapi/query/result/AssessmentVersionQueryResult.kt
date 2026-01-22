package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AnswersView
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.CollectionsView
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.FormVersion
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.PropertiesView
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierType
import java.time.LocalDateTime
import java.util.UUID

data class AssessmentVersionQueryResult(
  val assessmentUuid: UUID,
  val aggregateUuid: UUID,
  val assessmentType: String,
  val formVersion: FormVersion,
  val createdAt: LocalDateTime,
  val updatedAt: LocalDateTime,
  val answers: AnswersView,
  val properties: PropertiesView,
  val collections: CollectionsView,
  val collaborators: Set<User>,
  val identifiers: Map<IdentifierType, String>,
  val assignedUser: User?,
) : QueryResult
