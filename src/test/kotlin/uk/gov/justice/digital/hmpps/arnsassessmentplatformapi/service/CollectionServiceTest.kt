package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.CollectionRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.CollectionEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception.CollectionNotFoundException
import java.util.UUID

class CollectionServiceTest {
  val collectionRepository: CollectionRepository = mockk()
  val service = CollectionService(
    collectionRepository = collectionRepository,
  )

  @Nested
  inner class FindByUuid {
    @Test
    fun `it finds and returns the collection`() {
      val collection = CollectionEntity()

      every { collectionRepository.findByUuid(collection.uuid) } returns collection

      val result = service.findByUuid(collection.uuid)

      assertThat(result).isEqualTo(collection)
    }

    @Test
    fun `it throws when unable to find the collection`() {
      every { collectionRepository.findByUuid(any<UUID>()) } returns null

      assertThrows<CollectionNotFoundException> {
        service.findByUuid(UUID.randomUUID())
      }
    }
  }
}
