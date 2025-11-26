package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.source

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource

class ReplicationRoutingDataSource : AbstractRoutingDataSource() {
  public override fun determineCurrentLookupKey(): Any = DataSourceContextHolder.get()
}
