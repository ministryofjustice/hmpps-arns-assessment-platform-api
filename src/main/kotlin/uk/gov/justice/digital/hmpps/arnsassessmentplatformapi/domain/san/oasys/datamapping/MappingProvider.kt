package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.AnswersProvider
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.SectionMapping
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.exception.MappingNotFoundException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v1.Accommodation
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v1.AlcoholMisuse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v1.Attitudes
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v1.Drugs
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v1.Education
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v1.EmotionalWellbeing
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v1.FinancialManagement
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v1.LifestyleAssociates
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v1.NewSections
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v1.OffenceAnalysis
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v1.Predictors
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v1.Relationships
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v1.ThinkingBehaviours

@Component
class MappingProvider {
  fun get(formVersion: String, answersProvider: AnswersProvider): Set<SectionMapping> = (versions[formVersion] ?: throw MappingNotFoundException(formVersion))
    .map { factory -> factory(answersProvider) }
    .toSet()

  companion object {
    private val versions: Map<String, Set<(AnswersProvider) -> SectionMapping>> = mapOf(
      "v1.0" to setOf(
        ::Accommodation,
        ::AlcoholMisuse,
        ::Attitudes,
        ::Drugs,
        ::Education,
        ::EmotionalWellbeing,
        ::FinancialManagement,
        ::LifestyleAssociates,
        ::NewSections,
        ::OffenceAnalysis,
        ::Predictors,
        ::Relationships,
        ::ThinkingBehaviours,
      ),
    )
  }
}
