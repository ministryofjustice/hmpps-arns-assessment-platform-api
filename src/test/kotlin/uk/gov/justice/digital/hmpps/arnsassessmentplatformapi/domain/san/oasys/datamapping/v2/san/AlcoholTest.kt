package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v2.san

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Field
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Value
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.san.Given
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.san.SectionMappingTest
import kotlin.test.Test

class AlcoholTest : SectionMappingTest(Alcohol()) {
  @Test
  fun communityAlcoholEvidenceOfExcessDrinking() {
    test(
      Field.COMMUNITY_ALCOHOL_EVIDENCE_OF_EXCESS_DRINKING,
      Given().expect(null),
      Given("o9-2", "0").expect(Value.NO_EVIDENCE),
      Given("o9-2", "1").expect(Value.YES_WITH_SOME_EVIDENCE),
      Given("o9-2", "2").expect(Value.YES_WITH_EVIDENCE),
      Given("o9-2", "M").expect(null),
      Given("o9-2", null).expect(null),
    )
  }
}
