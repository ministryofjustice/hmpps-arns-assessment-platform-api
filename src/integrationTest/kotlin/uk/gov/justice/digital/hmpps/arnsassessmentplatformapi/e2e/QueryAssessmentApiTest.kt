package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.e2e

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.e2e.queries.QueryApiHelper.Companion.executeAssessmentQuery

@DisplayName("AAP API Tests")
class QueryAssessmentApiTest : IntegrationTestBase() {

  @Test
  fun `query assessment`() {
    val queryResponse = executeAssessmentQuery(webTestClient)
    assertThat(queryResponse?.queries).isNotEmpty()
  }
}
