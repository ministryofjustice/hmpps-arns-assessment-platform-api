package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.AnswersUpdated
import kotlin.test.assertIs

class UpdateAnswersTest {
  @Test
  fun `it can be transformed to an event`() {
    val event = UpdateAnswers(
      added = mapOf("foo" to listOf("foo_value")),
      removed = listOf("bar"),
    ).toEvent()

    assertIs<AnswersUpdated>(event)
  }
}
