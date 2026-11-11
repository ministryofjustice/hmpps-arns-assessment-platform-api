package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v2.san

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Field
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Value
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.san.Given
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.san.SectionMappingTest
import kotlin.test.Test

class AccommodationTest : SectionMappingTest(Accommodation()) {
  @Test
  fun currentAccommodation() {
    test(
      Field.CURRENT_ACCOMMODATION,
      Given().expect(null),
      Given("o1-test", 0).expect(Value.SETTLED),
      Given("o1-test", 1).expect(Value.TEMPORARY),
      Given("o1-test", 2).expect(Value.TEMPORARY),
      Given("o1-test", null).expect(null),
    )
  }
}
