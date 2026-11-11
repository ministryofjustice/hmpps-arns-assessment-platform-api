package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.exception

import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.Pathway
import java.util.UUID

class PathwayNotChangedException(assessmentUuid: UUID, pathway: Pathway) :
  AssessmentPlatformException(
    message = "Pathway has not changed",
    developerMessage = "Assessment with UUID: $assessmentUuid is already on $pathway pathway",
    statusCode = HttpStatus.BAD_REQUEST,
  )
