package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v2.san

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Field
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Value
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.san.Given
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.san.SectionMappingTest
import kotlin.test.Test

class AccommodationTest : SectionMappingTest(Accommodation()) {
  @Test
  fun communitySuitableHousing() {
    test(
      Field.COMMUNITY_SUITABLE_HOUSING,
      Given().expect(null),
      Given("o3-4", "0").expect(Value.YES),
      Given("o3-4", "1").expect(Value.YES_WITH_CONCERNS),
      Given("o3-4", "2").expect(Value.NO),
      Given("o3-4", "M").expect(null),
      Given("o3-4", null).expect(null),
    )
  }
}
