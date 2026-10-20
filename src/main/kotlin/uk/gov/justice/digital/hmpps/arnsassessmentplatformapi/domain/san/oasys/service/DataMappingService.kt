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
    val mapping = mappingProvider.get(formConfig.version, answersProvider)

    return mapping.fold(emptyMap<String, Any?>()) { acc, sectionMapping -> acc + sectionMapping.map() }
      .mapValues { (_, value) -> normaliseForOasys(value) }
  }

  // OASys can't store these multi-byte characters, so swap them for the plain versions
  // the original SAN service saved (via the HMPO form wizard default formatters)
  private fun normaliseForOasys(value: Any?): Any? = when (value) {
    is String ->
      value
        .replace(LONG_DASHES, "-")
        .replace(CURLY_APOSTROPHES, "'")
        .replace(CURLY_QUOTES, "\"")
    is List<*> -> value.map(::normaliseForOasys)
    is Map<*, *> -> value.mapValues { (_, nestedValue) -> normaliseForOasys(nestedValue) }
    else -> value
  }

  companion object {
    private val LONG_DASHES = Regex("[–—]")
    private val CURLY_APOSTROPHES = Regex("[‘’]")
    private val CURLY_QUOTES = Regex("[“”]")
  }
}
