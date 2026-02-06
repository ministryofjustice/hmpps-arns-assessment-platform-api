package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.criteria

import jakarta.persistence.criteria.JoinType
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity_
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentIdentifierEntity_
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierPair
import java.time.LocalDate
import java.time.LocalTime

data class AssessmentsByExternalIdentifiersCriteria(
  val externalIdentifiers: Set<IdentifierPair>,
  val from: LocalDate? = null,
  val to: LocalDate? = null,
) {
  private fun withExternalIdentifiers() = Specification { root, query, _ ->
    val identifiers = externalIdentifiers
      .takeIf { it.isNotEmpty() }
      ?: return@Specification null

    // Avoid duplicates caused by joins
    query.distinct(true)

    val join = root.join(
      AssessmentEntity_.identifiers,
      JoinType.INNER,
    )

    join
      .get(AssessmentIdentifierEntity_.externalIdentifier)
      .`in`(identifiers)
  }

  private fun fromTimestamp() = Specification { root, _, builder ->
    from?.let {
      builder.greaterThanOrEqualTo(
        root.get(AssessmentEntity_.createdAt),
        it.atTime(LocalTime.MIN),
      )
    }
  }

  private fun toTimestamp() = Specification { root, _, builder ->
    to?.let {
      builder.lessThanOrEqualTo(
        root.get(AssessmentEntity_.createdAt),
        it.atTime(LocalTime.MAX),
      )
    }
  }

  fun toSpecification(): Specification<AssessmentEntity> = withExternalIdentifiers()
    .and(fromTimestamp())
    .and(toTimestamp())
}
