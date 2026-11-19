package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v2.san

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Field
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Value
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.san.Given
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.san.SectionMappingTest
import kotlin.test.Test

class EmploymentEducationTest : SectionMappingTest(EmploymentEducation()) {
  @Test
  fun communityEmploymentHistory() {
    test(
      Field.COMMUNITY_EMPLOYMENT_HISTORY,
      Given().expect(null),
      Given("o4-3", "0").expect(Value.STABLE),
      Given("o4-3", "1").expect(Value.PERIODS_OF_INSTABILITY),
      Given("o4-3", "2").expect(Value.UNSTABLE),
      Given("o4-3", "M").expect(null),
      Given("o4-3", null).expect(null),
    )
  }

  @Test
  fun educationAcademicProfessionalVocationalQualifications() {
    test(
      Field.EDUCATION_ACADEMIC_PROFESSIONAL_VOCATIONAL_QUALIFICATIONS,
      Given().expect(null),
      Given("o4-9", "0").expect(Value.YES),
      Given("o4-9", "2").expect(Value.NO),
      Given("o4-9", "M").expect(null),
      Given("o4-9", null).expect(null),
    )
  }
}
