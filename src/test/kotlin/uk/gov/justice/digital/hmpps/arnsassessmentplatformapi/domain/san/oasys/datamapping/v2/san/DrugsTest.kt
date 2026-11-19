package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v2.san

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Field
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Value
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.san.Given
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.san.SectionMappingTest
import kotlin.test.Test

class DrugsTest : SectionMappingTest(Drugs()) {
  @Test
  fun drugUse() {
    test(
      Field.DRUG_USE,
      Given().expect(null),
      Given("o8-1", "YES").expect(Value.YES),
      Given("o8-1", "NO").expect(Value.NO),
      Given("o8-1", null).expect(null),
    )
  }

  @Test
  fun drugsPractitionerAnalysisMotivatedToStop() {
    test(
      Field.DRUGS_PRACTITIONER_ANALYSIS_MOTIVATED_TO_STOP,
      Given().expect(null),
      Given("o8-8", "0").expect(Value.FULL_MOTIVATION),
      Given("o8-8", "1").expect(Value.PARTIAL_MOTIVATION),
      Given("o8-8", "2").expect(Value.NO_MOTIVATION),
      Given("o8-8", "M").expect(null),
      Given("o8-8", null).expect(null),
    )
  }
}
