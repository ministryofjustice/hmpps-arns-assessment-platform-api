package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.CollectionRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.CollectionEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception.CollectionNotFoundException
import java.util.UUID

@Service
class CollectionService(
  private val collectionRepository: CollectionRepository,
) {
  fun findByUuid(collectionUuid: UUID) = collectionRepository.findByUuid(collectionUuid)
    ?: throw CollectionNotFoundException(collectionUuid)

  fun findRoot(collectionEntity: CollectionEntity) = if (collectionEntity.rootUuid == collectionEntity.uuid) {
    collectionEntity
  } else {
    findByUuid(collectionEntity.rootUuid)
  }
}
