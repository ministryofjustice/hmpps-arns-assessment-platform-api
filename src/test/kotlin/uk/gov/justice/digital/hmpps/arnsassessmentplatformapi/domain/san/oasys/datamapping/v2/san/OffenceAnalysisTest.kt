package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v2.san

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Field
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Value
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.san.Given
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.san.SectionMappingTest
import kotlin.test.Test

class OffenceAnalysisTest : SectionMappingTest(OffenceAnalysis()) {
  @Test
  fun offenceAnalysisDescriptionOfOffence() {
    test(
      Field.OFFENCE_ANALYSIS_DESCRIPTION_OF_OFFENCE,
      Given().expect(null),
      Given("o2-1", "Some free text").expect("Some free text"),
      Given("o2-1", null).expect(null),
    )
  }

  @Test
  fun offenceAnalysisReason() {
    test(
      Field.OFFENCE_ANALYSIS_REASON,
      Given().expect(null),
      Given("o2-8", "Some reason").expect("Some reason"),
      Given("o2-8", null).expect(null),
    )
  }

  @Test
  fun offenceAnalysisPatternsOfOffending() {
    test(
      Field.OFFENCE_ANALYSIS_PATTERNS_OF_OFFENDING,
      Given().expect(null),
      Given("o2-12", "Some patterns").expect("Some patterns"),
      Given("o2-12", null).expect(null),
    )
  }

  @Test
  fun offenceAnalysisImpactOnVictims() {
    test(
      Field.OFFENCE_ANALYSIS_IMPACT_ON_VICTIMS,
      Given().expect(null),
      Given("o2-6", "YES").expect(Value.YES),
      Given("o2-6", "NO").expect(Value.NO),
      Given("o2-6", null).expect(null),
    )
  }

  @Test
  fun offenceAnalysisAcceptResponsibility() {
    test(
      Field.OFFENCE_ANALYSIS_ACCEPT_RESPONSIBILITY,
      Given().expect(null),
      Given("o2-11", "YES").expect(Value.YES),
      Given("o2-11", "NO").expect(Value.NO),
      Given("o2-11", null).expect(null),
    )
  }

  @Test
  fun offenceAnalysisRisk() {
    test(
      Field.OFFENCE_ANALYSIS_RISK,
      Given().expect(null),
      Given("o2-99", "YES").expect(Value.YES),
      Given("o2-99", "NO").expect(Value.NO),
      Given("o2-99", null).expect(null),
    )
  }

  @Test
  fun offenceAnalysisHowManyInvolved() {
    test(
      Field.OFFENCE_ANALYSIS_HOW_MANY_INVOLVED,
      Given().expect(null),
      Given("o2-7", "YES").and("o2-7-1", "0").expect(Value.NONE),
      Given("o2-7", "YES").and("o2-7-1", "1").expect(Value.ONE),
      Given("o2-7", "YES").and("o2-7-1", "2").expect(Value.TWO),
      Given("o2-7", "YES").and("o2-7-1", "3").expect(Value.THREE),
      Given("o2-7", "YES").and("o2-7-1", "4").expect(Value.FOUR),
      Given("o2-7", "YES").and("o2-7-1", "5").expect(Value.FIVE),
      Given("o2-7", "YES").and("o2-7-1", "6-10").expect(Value.SIX_TO_10),
      Given("o2-7", "YES").and("o2-7-1", "11-15").expect(Value.ELEVEN_TO_15),
      Given("o2-7", "YES").and("o2-7-1", "15+").expect(Value.MORE_THAN_15),
      Given("o2-7", "NO").and("o2-7-1", "1").expect(null),
      Given("o2-7", null).expect(null),
    )
  }
}
