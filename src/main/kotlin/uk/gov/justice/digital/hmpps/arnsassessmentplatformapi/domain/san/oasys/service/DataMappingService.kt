package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregateView
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.formconfig.FormConfig
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.MappingProvider
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.AnswersProvider

typealias OasysEquivalent = Map<String, Any?>

@Service
class DataMappingService(
  val mappingProvider: MappingProvider,
) {
  fun getOasysEquivalent(assessmentAggregate: AssessmentAggregateView, formConfig: FormConfig): OasysEquivalent {
    val answersProvider = AnswersProvider(assessmentAggregate, formConfig)
    val mapping = mappingProvider.get(formConfig.version)

    return mapping.fold(emptyMap()) { acc, sectionMapping -> acc + sectionMapping.map(answersProvider) }
  }
}
