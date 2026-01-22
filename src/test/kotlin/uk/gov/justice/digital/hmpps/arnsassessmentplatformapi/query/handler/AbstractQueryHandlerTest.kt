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
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AuthSource
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.UserDetailsEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.Query
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.RequestableQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UuidIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.QueryResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.UserDetailsService
import java.time.LocalDateTime
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractQueryHandlerTest {
  val assessment = AssessmentEntity(type = "TEST")
  val assessmentService: AssessmentService = mockk()
  val stateService: StateService = mockk()
  val userDetailsService: UserDetailsService = mockk()
  val stateProvider: StateService.StateForType<AssessmentAggregate> = mockk()

  val user = UserDetails("FOO_USER", "Foo User")

  abstract val handler: KClass<out QueryHandler<out Query>>

  @BeforeEach
  fun setUp() {
    clearAllMocks()
  }

  fun test(query: RequestableQuery, aggregate: AggregateEntity<AssessmentAggregate>, expectedResult: QueryResult) {
    every { assessmentService.findBy(UuidIdentifier(assessment.uuid)) } returns assessment

    val state: AssessmentState = mockk()
    every { state.getForRead() } returns aggregate
    every { stateProvider.fetchOrCreateState(assessment, query.timestamp) } returns state
    every { stateService.stateForType(AssessmentAggregate::class) } returns stateProvider
    every { userDetailsService.findUsersByUuids(aggregate.data.collaborators) } returns aggregate.data.collaborators.mapIndexed { index, uuid ->
      UserDetailsEntity(
        index.toLong(),
        uuid,
        "user-$index",
        "User $index",
        AuthSource.NOT_SPECIFIED,
      )
    }.toSet()

    val handlerInstance = handler.primaryConstructor!!.call(
      assessmentService,
      stateService,
      userDetailsService,
    )

    assertThat(handlerInstance.type).isEqualTo(query::class)

    val result = handlerInstance.execute(query)

    verify(exactly = 1) { assessmentService.findBy(UuidIdentifier(assessment.uuid)) }
    verify(exactly = 1) { state.getForRead() }
    verify(exactly = 1) { stateProvider.fetchOrCreateState(assessment, query.timestamp) }
    verify(exactly = 1) { stateService.stateForType(AssessmentAggregate::class) }

    assertThat(result).isEqualTo(expectedResult)
  }

  fun testThrows(
    query: RequestableQuery,
    aggregate: AggregateEntity<AssessmentAggregate>,
    expectedError: AssessmentPlatformException,
  ) {
    every { assessmentService.findBy(UuidIdentifier(assessment.uuid)) } returns assessment

    val state: AssessmentState = mockk()
    every { state.getForRead() } returns aggregate
    every { stateProvider.fetchOrCreateState(assessment, query.timestamp) } returns state
    every { stateService.stateForType(AssessmentAggregate::class) } returns stateProvider

    val handlerInstance = handler.primaryConstructor!!.call(
      assessmentService,
      stateService,
      userDetailsService,
    )

    assertThat(handlerInstance.type).isEqualTo(query::class)

    val error = assertThrows<AssessmentPlatformException> { handlerInstance.execute(query) }

    verify(exactly = 1) { assessmentService.findBy(UuidIdentifier(assessment.uuid)) }
    verify(exactly = 1) { state.getForRead() }
    verify(exactly = 1) { stateProvider.fetchOrCreateState(assessment, query.timestamp) }
    verify(exactly = 1) { stateService.stateForType(AssessmentAggregate::class) }

    assertThat(error.message).isEqualTo(expectedError.message)
  }

  fun timestampProvider() = listOf(
    Arguments.of(null),
    Arguments.of(LocalDateTime.parse("2020-06-01T10:42:43")),
  )
}
