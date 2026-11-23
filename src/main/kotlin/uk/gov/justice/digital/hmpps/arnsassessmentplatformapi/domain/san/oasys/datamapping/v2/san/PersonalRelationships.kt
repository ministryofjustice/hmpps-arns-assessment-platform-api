package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v2.san

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Field
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Value
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.SanSectionMapping
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.exception.UnexpectedOasysValueException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.service.OasysEquivalent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue

class PersonalRelationships : SanSectionMapping() {
  override fun map(oasys: OasysEquivalent, aap: AssessmentAggregate) {
    when (oasys["o6-9"]) {
      "YES" -> Value.YES
      "NO" -> Value.NO
      null -> null
      else -> throw UnexpectedOasysValueException("o6-9")
    }?.let {
      aap.answers[Field.PERSONAL_RELATIONSHIPS_COMMUNITY_PARENTAL_RESPONSIBILITIES.lower] = SingleValue(it.name)
    }

    when (oasys["o6-1"]) {
      "0" -> Value.STABLE_RELATIONSHIP
      "1" -> Value.MIXED_RELATIONSHIP
      "2" -> Value.UNSTABLE_RELATIONSHIP
      "M", null -> null
      else -> throw UnexpectedOasysValueException("o6-1")
    }?.let {
      aap.answers[Field.PERSONAL_RELATIONSHIPS_COMMUNITY_FAMILY_RELATIONSHIP.lower] = SingleValue(it.name)
    }

    when (oasys["o6-3"]) {
      "0" -> Value.POSITIVE_CHILDHOOD
      "1" -> Value.MIXED_CHILDHOOD
      "2" -> Value.NEGATIVE_CHILDHOOD
      "M", null -> null
      else -> throw UnexpectedOasysValueException("o6-3")
    }?.let {
      aap.answers[Field.PERSONAL_RELATIONSHIPS_COMMUNITY_CHILDHOOD.lower] = SingleValue(it.name)
    }

    when (oasys["o10-7_V2_CHILDHOOD"]) {
      "YES" -> Value.YES
      "NO" -> Value.NO
      null -> null
      else -> throw UnexpectedOasysValueException("o10-7_V2_CHILDHOOD")
    }?.let {
      aap.answers[Field.PERSONAL_RELATIONSHIPS_COMMUNITY_CHILDHOOD_BEHAVIOUR.lower] = SingleValue(it.name)
    }

    when (oasys["o6-8"]) {
      "1" -> Value.IN_A_RELATIONSHIP_LIVING_TOGETHER
      "2" -> Value.IN_A_RELATIONSHIP_NOT_LIVING_TOGETHER
      "3" -> Value.NOT_IN_A_RELATIONSHIP
      null -> null
      else -> throw UnexpectedOasysValueException("o6-8")
    }?.let {
      aap.answers[Field.PERSONAL_RELATIONSHIPS_COMMUNITY_RELATIONSHIP_STATUS.lower] = SingleValue(it.name)
    }

    when (oasys["o6-4"]) {
      "0" -> Value.HAPPY_RELATIONSHIP
      "1" -> Value.CONCERNS_HAPPY_RELATIONSHIP
      "2" -> Value.UNHAPPY_RELATIONSHIP
      "M", null -> null
      else -> throw UnexpectedOasysValueException("o6-4")
    }?.let {
      aap.answers[Field.PERSONAL_RELATIONSHIPS_COMMUNITY_CURRENT_RELATIONSHIP.lower] = SingleValue(it.name)
    }
  }
}
