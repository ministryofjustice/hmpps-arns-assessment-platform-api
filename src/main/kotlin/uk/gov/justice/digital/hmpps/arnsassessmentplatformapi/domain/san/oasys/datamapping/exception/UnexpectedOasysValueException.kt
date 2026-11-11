package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.exception

class UnexpectedOasysValueException(field: String) : RuntimeException("Unexpected OASys value for field $field")
