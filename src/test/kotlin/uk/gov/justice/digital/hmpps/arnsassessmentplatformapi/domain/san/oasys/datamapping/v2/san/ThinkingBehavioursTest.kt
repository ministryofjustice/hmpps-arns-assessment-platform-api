package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v2.san

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Field
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Value
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.san.Given
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.san.SectionMappingTest
import kotlin.test.Test

class ThinkingBehavioursTest : SectionMappingTest(ThinkingBehaviours()) {
  @Test
  fun thinkingBehavioursAttitudesConsequences() {
    test(
      Field.THINKING_BEHAVIOURS_ATTITUDES_CONSEQUENCES,
      Given().expect(null),
      Given("o11-7", "0").expect(Value.YES),
      Given("o11-7", "1").expect(Value.SOMETIMES),
      Given("o11-7", "2").expect(Value.NO),
      Given("o11-7", null).expect(null),
    )
  }

  @Test
  fun thinkingBehavioursAttitudesProblemSolving() {
    test(
      Field.THINKING_BEHAVIOURS_ATTITUDES_PROBLEM_SOLVING,
      Given().expect(null),
      Given("o11-6", "0").expect(Value.YES),
      Given("o11-6", "1").expect(Value.LIMITED_PROBLEM_SOLVING),
      Given("o11-6", "2").expect(Value.NO),
      Given("o11-6", null).expect(null),
    )
  }

  @Test
  fun thinkingBehavioursAttitudesPeoplesViews() {
    test(
      Field.THINKING_BEHAVIOURS_ATTITUDES_PEOPLES_VIEWS,
      Given().expect(null),
      Given("o11-9", "0").expect(Value.YES),
      Given("o11-9", "1").expect(Value.SOMETIMES),
      Given("o11-9", "2").expect(Value.NO),
      Given("o11-9", null).expect(null),
    )
  }

  @Test
  fun thinkingBehavioursAttitudesTemperManagement() {
    test(
      Field.THINKING_BEHAVIOURS_ATTITUDES_TEMPER_MANAGEMENT,
      Given().expect(null),
      Given("o11-4", "0").expect(Value.YES),
      Given("o11-4", "1").expect(Value.SOMETIMES),
      Given("o11-4", "2").expect(Value.NO),
      Given("o11-4", null).expect(null),
    )
  }

  @Test
  fun thinkingBehavioursAttitudesImpulsiveBehaviour() {
    test(
      Field.THINKING_BEHAVIOURS_ATTITUDES_IMPULSIVE_BEHAVIOUR,
      Given().expect(null),
      Given("o11-2", "0").expect(Value.NO),
      Given("o11-2", "1").expect(Value.SOMETIMES),
      Given("o11-2", "2").expect(Value.YES),
      Given("o11-2", null).expect(null),
    )
  }

  @Test
  fun thinkingBehavioursAttitudesHostileOrientation() {
    test(
      Field.THINKING_BEHAVIOURS_ATTITUDES_HOSTILE_ORIENTATION,
      Given().expect(null),
      Given("o12-9", "0").expect(Value.NO),
      Given("o12-9", "1").expect(Value.SOME),
      Given("o12-9", "2").expect(Value.YES),
      Given("o12-9", null).expect(null),
    )
  }

  @Test
  fun communityThinkingBehavioursAttitudesSupervision() {
    test(
      Field.COMMUNITY_THINKING_BEHAVIOURS_ATTITUDES_SUPERVISION,
      Given().expect(null),
      Given("o12-4", "0").expect(Value.YES_SUPERVISION),
      Given("o12-4", "1").expect(Value.UNSURE_SUPERVISION),
      Given("o12-4", "2").expect(Value.NO_SUPERVISION),
      Given("o12-4", null).expect(null),
    )
  }

  @Test
  fun thinkingBehavioursAttitudesCriminalBehaviour() {
    test(
      Field.THINKING_BEHAVIOURS_ATTITUDES_CRIMINAL_BEHAVIOUR,
      Given().expect(null),
      Given("o12-1", "0").expect(Value.NO),
      Given("o12-1", "1").expect(Value.SOMETIMES),
      Given("o12-1", "2").expect(Value.YES),
      Given("o12-1", null).expect(null),
    )
  }

  @Test
  fun thinkingBehavioursAttitudesRiskSexualHarm() {
    test(
      Field.THINKING_BEHAVIOURS_ATTITUDES_RISK_SEXUAL_HARM,
      Given().expect(null),
      Given("o1-30", "YES").expect(Value.YES),
      Given("o1-30", "NO").expect(null),
      Given("o1-30", null).expect(null),
    )
  }
}
