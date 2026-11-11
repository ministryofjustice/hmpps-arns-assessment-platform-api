package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.exception

class MappingNotFoundException(type: String, version: String) : RuntimeException("No $type data mapping found for form version $version")
