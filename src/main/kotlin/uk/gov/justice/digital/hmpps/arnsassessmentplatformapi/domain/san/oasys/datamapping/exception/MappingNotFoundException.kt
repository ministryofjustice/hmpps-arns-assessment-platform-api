package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.exception

class MappingNotFoundException(version: String) : RuntimeException("No data mapping found for form version $version")
