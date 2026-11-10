package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v1

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.SanToOasysMappingVersion
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v1.oasys.Accommodation
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v1.oasys.AlcoholMisuse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v1.oasys.Attitudes
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v1.oasys.Drugs
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v1.oasys.Education
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v1.oasys.EmotionalWellbeing
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v1.oasys.FinancialManagement
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v1.oasys.LifestyleAssociates
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v1.oasys.NewSections
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v1.oasys.OffenceAnalysis
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v1.oasys.Predictors
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v1.oasys.Relationships
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.v1.oasys.ThinkingBehaviours

val SanToOasysV1: SanToOasysMappingVersion = "v1.0" to setOf(
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
)
