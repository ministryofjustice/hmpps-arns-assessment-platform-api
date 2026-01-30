package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.criteria

import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity_
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity_
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.UserDetailsEntity_
import java.time.LocalDateTime
import java.util.UUID

data class TimelineCriteria(
  val assessmentUuid: UUID? = null,
  val userUuid: UUID? = null,
  val from: LocalDateTime? = null,
  val to: LocalDateTime? = null,
  val includeEventTypes: Set<String>? = null,
  val excludeEventTypes: Set<String>? = null,
  val includeCustomTypes: Set<String>? = null,
  val excludeCustomTypes: Set<String>? = null,
) {
  fun getSpecification(): Specification<TimelineEntity> {
    if (listOf(assessmentUuid, userUuid).all { it == null }) {
      throw RuntimeException("Must specify at least one of assessmentUuid or userUuid")
    }

    return forAssessment()
      .and(fromUser())
      .and(fromTimestamp())
      .and(toTimestamp())
      .and(includeEventTypes())
      .and(excludeEventTypes())
      .and(includeCustomTypes())
      .and(excludeCustomTypes())
  }

  private fun forAssessment() = Specification<TimelineEntity> { root, _, builder ->
    assessmentUuid?.let {
      builder.equal(root.get(TimelineEntity_.assessment).get(AssessmentEntity_.uuid), assessmentUuid)
    }
  }

  private fun fromUser() = Specification<TimelineEntity> { root, _, builder ->
    userUuid?.let {
      builder.equal(root.get(TimelineEntity_.user).get(UserDetailsEntity_.uuid), userUuid)
    }
  }

  private fun fromTimestamp() = Specification<TimelineEntity> { root, _, builder ->
    from?.let {
      builder.greaterThanOrEqualTo(root.get(TimelineEntity_.createdAt), from)
    }
  }

  private fun toTimestamp() = Specification<TimelineEntity> { root, _, builder ->
    to?.let {
      builder.lessThanOrEqualTo(root.get(TimelineEntity_.createdAt), to)
    }
  }

  private fun includeEventTypes() = Specification<TimelineEntity> { root, _, builder ->
    includeEventTypes?.takeIf { it.isNotEmpty() }?.let { builder.and(root.get(TimelineEntity_.eventType).`in`(it)) }
  }

  private fun excludeEventTypes() = Specification<TimelineEntity> { root, _, builder ->
    excludeEventTypes?.takeIf { it.isNotEmpty() }?.let { builder.not(root.get(TimelineEntity_.eventType).`in`(it)) }
  }

  private fun includeCustomTypes() = Specification<TimelineEntity> { root, _, builder ->
    includeCustomTypes?.takeIf { it.isNotEmpty() }?.let { builder.and(root.get(TimelineEntity_.customType).`in`(it)) }
  }

  private fun excludeCustomTypes() = Specification<TimelineEntity> { root, _, builder ->
    excludeCustomTypes?.takeIf { it.isNotEmpty() }?.let { builder.not(root.get(TimelineEntity_.customType).`in`(it)) }
  }
}
