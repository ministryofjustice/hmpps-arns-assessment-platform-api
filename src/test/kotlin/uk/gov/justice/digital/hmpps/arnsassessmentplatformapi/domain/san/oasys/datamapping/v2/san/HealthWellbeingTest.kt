package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v2.san

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Field
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Value
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.san.Given
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.san.SectionMappingTest
import kotlin.test.Test

class HealthWellbeingTest : SectionMappingTest(HealthWellbeing()) {
  @Test
  fun healthWellbeingLearningDifficulties() {
    test(
      Field.HEALTH_WELLBEING_LEARNING_DIFFICULTIES,
      Given().expect(null),
      Given("o4-8", "0").expect(Value.NO),
      Given("o4-8", "1").expect(Value.YES_SOME_DIFFICULTIES),
      Given("o4-8", "2").expect(Value.YES_SIGNIFICANT),
      Given("o4-8", "M").expect(null),
      Given("o4-8", null).expect(null),
    )
  }

  @Test
  fun healthWellbeingCopingDayToDayLife() {
    test(
      Field.HEALTH_WELLBEING_COPING_DAY_TO_DAY_LIFE,
      Given().expect(null),
      Given("o10-1", "0").expect(Value.YES),
      Given("o10-1", "1").expect(Value.YES_SOME_DIFFICULTIES),
      Given("o10-1", "2").expect(Value.NO),
      Given("o10-1", null).expect(null),
    )
  }

  @Test
  fun healthWellbeingAttitudeTowardsSelf() {
    test(
      Field.HEALTH_WELLBEING_ATTITUDE_TOWARDS_SELF,
      Given().expect(null),
      Given("o10-4", "0").expect(Value.POSITIVE),
      Given("o10-4", "1").expect(Value.SOME_NEGATIVE_ASPECTS),
      Given("o10-4", "2").expect(Value.NEGATIVE),
      Given("o10-4", null).expect(null),
    )
  }
}
