package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.Query
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.QueryResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.TimelineService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.UserDetailsService
import kotlin.reflect.KClass
import kotlin.reflect.cast

@Component
class QueryHandlerServiceBundle(
  val assessment: AssessmentService,
  val state: StateService,
  val userDetails: UserDetailsService,
  val timeline: TimelineService,
)

interface QueryHandler<C : Query> {
  val type: KClass<C>
  fun handle(query: C): QueryResult
  fun execute(query: Query) = handle(type.cast(query))
}
