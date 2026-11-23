package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v2.san

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Field
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Value
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.san.Given
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.san.SectionMappingTest
import kotlin.test.Test

class PersonalRelationshipsTest : SectionMappingTest(PersonalRelationships()) {
  @Test
  fun personalRelationshipsCommunityParentalResponsibilities() {
    test(
      Field.PERSONAL_RELATIONSHIPS_COMMUNITY_PARENTAL_RESPONSIBILITIES,
      Given().expect(null),
      Given("o6-9", "YES").expect(Value.YES),
      Given("o6-9", "NO").expect(Value.NO),
      Given("o6-9", null).expect(null),
    )
  }

  @Test
  fun personalRelationshipsCommunityFamilyRelationship() {
    test(
      Field.PERSONAL_RELATIONSHIPS_COMMUNITY_FAMILY_RELATIONSHIP,
      Given().expect(null),
      Given("o6-1", "0").expect(Value.STABLE_RELATIONSHIP),
      Given("o6-1", "1").expect(Value.MIXED_RELATIONSHIP),
      Given("o6-1", "2").expect(Value.UNSTABLE_RELATIONSHIP),
      Given("o6-1", "M").expect(null),
      Given("o6-1", null).expect(null),
    )
  }

  @Test
  fun personalRelationshipsCommunityChildhood() {
    test(
      Field.PERSONAL_RELATIONSHIPS_COMMUNITY_CHILDHOOD,
      Given().expect(null),
      Given("o6-3", "0").expect(Value.POSITIVE_CHILDHOOD),
      Given("o6-3", "1").expect(Value.MIXED_CHILDHOOD),
      Given("o6-3", "2").expect(Value.NEGATIVE_CHILDHOOD),
      Given("o6-3", "M").expect(null),
      Given("o6-3", null).expect(null),
    )
  }

  @Test
  fun personalRelationshipsCommunityChildhoodBehaviour() {
    test(
      Field.PERSONAL_RELATIONSHIPS_COMMUNITY_CHILDHOOD_BEHAVIOUR,
      Given().expect(null),
      Given("o10-7_V2_CHILDHOOD", "YES").expect(Value.YES),
      Given("o10-7_V2_CHILDHOOD", "NO").expect(Value.NO),
      Given("o10-7_V2_CHILDHOOD", null).expect(null),
    )
  }

  @Test
  fun personalRelationshipsCommunityRelationshipStatus() {
    test(
      Field.PERSONAL_RELATIONSHIPS_COMMUNITY_RELATIONSHIP_STATUS,
      Given().expect(null),
      Given("o6-8", "1").expect(Value.IN_A_RELATIONSHIP_LIVING_TOGETHER),
      Given("o6-8", "2").expect(Value.IN_A_RELATIONSHIP_NOT_LIVING_TOGETHER),
      Given("o6-8", "3").expect(Value.NOT_IN_A_RELATIONSHIP),
      Given("o6-8", null).expect(null),
    )
  }

  @Test
  fun personalRelationshipsCommunityCurrentRelationship() {
    test(
      Field.PERSONAL_RELATIONSHIPS_COMMUNITY_CURRENT_RELATIONSHIP,
      Given().expect(null),
      Given("o6-4", "0").expect(Value.HAPPY_RELATIONSHIP),
      Given("o6-4", "1").expect(Value.CONCERNS_HAPPY_RELATIONSHIP),
      Given("o6-4", "2").expect(Value.UNHAPPY_RELATIONSHIP),
      Given("o6-4", "M").expect(null),
      Given("o6-4", null).expect(null),
    )
  }
}
