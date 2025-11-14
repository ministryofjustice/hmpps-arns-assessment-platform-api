package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentAggregate
import java.time.LocalDateTime
import kotlin.test.Test

class AggregateEntityTest {
  @Test
  fun `aggregate entity is cloned`() {
    val assessment = AssessmentEntity()

    val dataAggregate: AssessmentAggregate = mockk()
    val clonedAggregate: AssessmentAggregate = mockk()

    every { dataAggregate.clone() } returns clonedAggregate

    val aggregate = AggregateEntity(
      assessment = assessment,
      eventsFrom = LocalDateTime.now().minusDays(1),
      eventsTo = LocalDateTime.now(),
      data = dataAggregate,
    )

    val clone = aggregate.clone()

    assertThat(clone.uuid).isNotEqualTo(aggregate.uuid)
    assertThat(clone.assessment).isEqualTo(aggregate.assessment)
    assertThat(clone.eventsFrom).isEqualTo(aggregate.eventsFrom)
    assertThat(clone.eventsTo).isEqualTo(aggregate.eventsTo)
    assertThat(clone.data).isEqualTo(clonedAggregate)

    verify(exactly = 1) { dataAggregate.clone() }
  }
}
