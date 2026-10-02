package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.e2e

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.e2e.queries.QueryApiHelper.executeAssessmentQuery
import java.util.UUID

@DisplayName("AAP API Tests")
class QueryAssessmentApiTest : IntegrationTestBase() {
  val assessmentId: UUID = UUID.fromString(System.getenv("AAP_API_ASSESSMENT") ?: "caff2f14-a083-41f0-8d26-638b30177511")

  @Test
  fun `query assessment`() {
    val queryResponse = executeAssessmentQuery(webTestClient, assessmentId)
    assertThat(queryResponse?.queries).isNotEmpty()
  }
}
