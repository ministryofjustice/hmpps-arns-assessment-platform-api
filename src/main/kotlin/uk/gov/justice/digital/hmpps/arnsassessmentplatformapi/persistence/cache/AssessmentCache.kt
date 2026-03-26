package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.cache

import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import java.util.UUID

@Component
@RequestScope
class AssessmentCache {
  private val assessments = mutableMapOf<UUID, AssessmentEntity>()

  fun get(uuid: UUID) = assessments[uuid]
  fun put(assessment: AssessmentEntity) = assessments.put(assessment.uuid, assessment).let { assessment }
}
