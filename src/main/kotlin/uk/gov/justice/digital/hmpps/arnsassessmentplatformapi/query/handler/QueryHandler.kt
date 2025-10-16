package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.Query
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.QueryResult
import kotlin.reflect.KClass
import kotlin.reflect.cast

interface QueryHandler<C : Query> {
  val type: KClass<C>
  fun handle(query: C): QueryResult
  fun execute(query: Query) = handle(type.cast(query))
}
