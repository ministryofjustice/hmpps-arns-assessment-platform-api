package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierPair
import java.time.LocalDate
import java.time.LocalDateTime

data class SubjectAccessRequestQuery(
  override val timestamp: LocalDateTime? = null,
  val assessmentIdentifiers: Set<IdentifierPair>,
  val from: LocalDate? = null,
  val to: LocalDate? = null,
) : Query
