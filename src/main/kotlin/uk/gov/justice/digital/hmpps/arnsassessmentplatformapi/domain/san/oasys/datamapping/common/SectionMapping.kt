package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.service.OasysEquivalent

typealias MappingFn = () -> Any?
typealias FieldsToMap = Map<String, MappingFn>

abstract class SectionMapping(protected val ap: AnswersProvider) {
  abstract fun getFieldsToMap(): FieldsToMap

  fun map(): OasysEquivalent {
    val result = mutableMapOf<String, Any?>()
    for ((field, method) in getFieldsToMap()) {
      result[field] = method()
    }
    return result
  }
}
