package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.exception.HandlerNotImplementedException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.Query
import kotlin.reflect.KClass

@Component
class QueryHandlerRegistry(
  handlers: List<QueryHandler<out Query>>,
) {
  private val registry: Map<KClass<out Query>, QueryHandler<out Query>> = handlers.associateBy { it.type }

  fun getHandlerFor(query: KClass<out Query>) = registry[query] ?: throw HandlerNotImplementedException("No handler registered for type: ${query.simpleName}")
}
