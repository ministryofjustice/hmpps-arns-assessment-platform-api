package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.bus

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.Query
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.RequestableQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AuditService

@Service
class QueryBus(
  private val registry: QueryHandlerRegistry,
  private val auditService: AuditService,
) {
  fun dispatch(query: Query) = registry.getHandlerFor(query::class).execute(query)
    .also { if (query is RequestableQuery) auditService.audit(query) }
}
