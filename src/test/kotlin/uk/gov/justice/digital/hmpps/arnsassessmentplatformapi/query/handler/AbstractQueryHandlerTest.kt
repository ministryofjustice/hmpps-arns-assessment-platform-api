package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.provider.Arguments
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.Query
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.RequestableQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.QueryResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService
import java.time.LocalDateTime
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractQueryHandlerTest {
  val assessment = AssessmentEntity()
  val assessmentService: AssessmentService = mockk()
  val stateService: StateService = mockk()
  val stateProvider: StateService.StateForType<AssessmentAggregate> = mockk()

  val user = User("FOO_USER", "Foo User")

  abstract val handler: KClass<out QueryHandler<out Query>>

  @BeforeEach
  fun setUp() {
    clearAllMocks()
  }

  fun test(query: RequestableQuery, aggregate: AggregateEntity<AssessmentAggregate>, expectedResult: QueryResult) {
    every { assessmentService.findByUuid(assessment.uuid) } returns assessment

    val state: AssessmentState = mockk()
    every { state.get() } returns aggregate
    every { stateProvider.fetchOrCreateState(assessment, query.timestamp) } returns state
    every { stateService.stateForType(AssessmentAggregate::class) } returns stateProvider

    val handlerInstance = handler.primaryConstructor!!.call(
      assessmentService,
      stateService,
    )

    assertThat(handlerInstance.type).isEqualTo(query::class)

    val result = handlerInstance.execute(query)

    verify(exactly = 1) { assessmentService.findByUuid(assessment.uuid) }
    verify(exactly = 1) { state.get() }
    verify(exactly = 1) { stateProvider.fetchOrCreateState(assessment, query.timestamp) }
    verify(exactly = 1) { stateService.stateForType(AssessmentAggregate::class) }

    assertThat(result).isEqualTo(expectedResult)
  }

  fun testThrows(query: RequestableQuery, aggregate: AggregateEntity<AssessmentAggregate>, expectedError: Error) {
    every { assessmentService.findByUuid(assessment.uuid) } returns assessment

    val state: AssessmentState = mockk()
    every { state.get() } returns aggregate
    every { stateProvider.fetchOrCreateState(assessment, query.timestamp) } returns state
    every { stateService.stateForType(AssessmentAggregate::class) } returns stateProvider

    val handlerInstance = handler.primaryConstructor!!.call(
      assessmentService,
      stateService,
    )

    assertThat(handlerInstance.type).isEqualTo(query::class)

    val error = assertThrows<Error> { handlerInstance.execute(query) }

    verify(exactly = 1) { assessmentService.findByUuid(assessment.uuid) }
    verify(exactly = 1) { state.get() }
    verify(exactly = 1) { stateProvider.fetchOrCreateState(assessment, query.timestamp) }
    verify(exactly = 1) { stateService.stateForType(AssessmentAggregate::class) }

    assertThat(error.message).isEqualTo(expectedError.message)
  }

  fun timestampProvider() = listOf(
    Arguments.of(null),
    Arguments.of(LocalDateTime.parse("2020-06-01T10:42:43")),
  )
}
