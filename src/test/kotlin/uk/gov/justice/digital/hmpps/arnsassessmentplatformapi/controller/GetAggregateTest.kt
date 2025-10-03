package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AggregateRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.EventRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity

class GetAggregateTest(
  @Autowired
  private val assessmentRepository: AssessmentRepository,
  @Autowired
  private val aggregateRepository: AggregateRepository,
  @Autowired
  private val eventRepository: EventRepository,
) : IntegrationTestBase() {
  @BeforeEach
  fun setUp() {
  }

  @AfterEach
  fun tearDown() {
  }

  @Test
  fun `it fetches the latest aggregate for an assessment`() {
    val assessment: AssessmentEntity = assessmentRepository.save(AssessmentEntity())
  }
  fun `it fetches an aggregate for a point in time`() {}
  fun `it creates an aggregate for an assessment where none exists`() {}
  fun `it returns 404 when there is no assessments`() {}
  fun `it returns 400 when an aggregate does not exist for a given type`() {}
}
