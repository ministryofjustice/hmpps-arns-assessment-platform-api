package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v2.san

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Field
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Value
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.SanSectionMapping
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.exception.UnexpectedOasysValueException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.service.OasysEquivalent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue

class ThinkingBehaviours : SanSectionMapping() {
  override fun map(oasys: OasysEquivalent, aap: AssessmentAggregate) {
    when (oasys["o11-7"]) {
      "0" -> Value.YES
      "1" -> Value.SOMETIMES
      "2" -> Value.NO
      null -> null
      else -> throw UnexpectedOasysValueException("o11-7")
    }?.let {
      aap.answers[Field.THINKING_BEHAVIOURS_ATTITUDES_CONSEQUENCES.lower] = SingleValue(it.name)
    }

    when (oasys["o11-6"]) {
      "0" -> Value.YES
      "1" -> Value.LIMITED_PROBLEM_SOLVING
      "2" -> Value.NO
      null -> null
      else -> throw UnexpectedOasysValueException("o11-6")
    }?.let {
      aap.answers[Field.THINKING_BEHAVIOURS_ATTITUDES_PROBLEM_SOLVING.lower] = SingleValue(it.name)
    }

    when (oasys["o11-9"]) {
      "0" -> Value.YES
      "1" -> Value.SOMETIMES
      "2" -> Value.NO
      null -> null
      else -> throw UnexpectedOasysValueException("o11-9")
    }?.let {
      aap.answers[Field.THINKING_BEHAVIOURS_ATTITUDES_PEOPLES_VIEWS.lower] = SingleValue(it.name)
    }

    when (oasys["o11-4"]) {
      "0" -> Value.YES
      "1" -> Value.SOMETIMES
      "2" -> Value.NO
      null -> null
      else -> throw UnexpectedOasysValueException("o11-4")
    }?.let {
      aap.answers[Field.THINKING_BEHAVIOURS_ATTITUDES_TEMPER_MANAGEMENT.lower] = SingleValue(it.name)
    }

    when (oasys["o11-2"]) {
      "0" -> Value.NO
      "1" -> Value.SOMETIMES
      "2" -> Value.YES
      null -> null
      else -> throw UnexpectedOasysValueException("o11-2")
    }?.let {
      aap.answers[Field.THINKING_BEHAVIOURS_ATTITUDES_IMPULSIVE_BEHAVIOUR.lower] = SingleValue(it.name)
    }

    when (oasys["o12-9"]) {
      "0" -> Value.NO
      "1" -> Value.SOME
      "2" -> Value.YES
      null -> null
      else -> throw UnexpectedOasysValueException("o12-9")
    }?.let {
      aap.answers[Field.THINKING_BEHAVIOURS_ATTITUDES_HOSTILE_ORIENTATION.lower] = SingleValue(it.name)
    }

    when (oasys["o12-4"]) {
      "0" -> Value.YES_SUPERVISION
      "1" -> Value.UNSURE_SUPERVISION
      "2" -> Value.NO_SUPERVISION
      null -> null
      else -> throw UnexpectedOasysValueException("o12-4")
    }?.let {
      aap.answers[Field.COMMUNITY_THINKING_BEHAVIOURS_ATTITUDES_SUPERVISION.lower] = SingleValue(it.name)
    }

    when (oasys["o12-1"]) {
      "0" -> Value.NO
      "1" -> Value.SOMETIMES
      "2" -> Value.YES
      null -> null
      else -> throw UnexpectedOasysValueException("o12-1")
    }?.let {
      aap.answers[Field.THINKING_BEHAVIOURS_ATTITUDES_CRIMINAL_BEHAVIOUR.lower] = SingleValue(it.name)
    }

    when (oasys["o1-30"]) {
      "YES" -> Value.YES
      "NO", null -> null
      else -> throw UnexpectedOasysValueException("o1-30")
    }?.let {
      aap.answers[Field.THINKING_BEHAVIOURS_ATTITUDES_RISK_SEXUAL_HARM.lower] = SingleValue(it.name)
    }
  }
}
