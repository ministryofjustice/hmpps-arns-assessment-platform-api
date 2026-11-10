package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.AnswersProvider
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.OasysSectionMapping
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.OasysSectionMappingFactory
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.SanSectionMapping
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.exception.MappingNotFoundException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v1.SanToOasysV1
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v2.OasysToSanV2
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v2.SanToOasysV2

@Component
class MappingProvider {
  fun get(formVersion: String, answersProvider: AnswersProvider): Set<OasysSectionMapping> = (sanToOasys[formVersion] ?: throw MappingNotFoundException("SAN->OASys", formVersion))
    .map { factory -> factory(answersProvider) }
    .toSet()

  fun get(formVersion: String): Set<SanSectionMapping> = (oasysToSan[formVersion] ?: throw MappingNotFoundException("OASys->SAN", formVersion))

  companion object {
    private val sanToOasys: Map<String, Set<OasysSectionMappingFactory>> = mapOf(
      SanToOasysV1,
      SanToOasysV2,
    )

    private val oasysToSan: Map<String, Set<SanSectionMapping>> = mapOf(
      OasysToSanV2,
    )
  }
}
