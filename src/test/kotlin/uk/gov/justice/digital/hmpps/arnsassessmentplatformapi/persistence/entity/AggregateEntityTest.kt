package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock.Clock
import java.time.LocalDateTime
import kotlin.test.Test

class AggregateEntityTest {
  val clock: Clock = mockk()

  @BeforeEach
  fun setUp() {
    every { clock.now() } returns LocalDateTime.now()
  }

  @Test
  fun `aggregate entity is cloned`() {
    val assessment = AssessmentEntity(type = "TEST", createdAt = clock.now())

    val dataAggregate: AssessmentAggregate = mockk()
    val clonedAggregate: AssessmentAggregate = mockk()

    every { dataAggregate.clone() } returns clonedAggregate

    val aggregate = AggregateEntity(
      assessment = assessment,
      eventsFrom = clock.now().minusDays(1),
      eventsTo = clock.now(),
      updatedAt = clock.now(),
      data = dataAggregate,
    )

    val clone = aggregate.clone(clock)

    assertThat(clone.uuid).isNotEqualTo(aggregate.uuid)
    assertThat(clone.assessment).isEqualTo(aggregate.assessment)
    assertThat(clone.eventsFrom).isEqualTo(aggregate.eventsFrom)
    assertThat(clone.eventsTo).isEqualTo(aggregate.eventsTo)
    assertThat(clone.data).isEqualTo(clonedAggregate)

    verify(exactly = 1) { dataAggregate.clone() }
  }
}
