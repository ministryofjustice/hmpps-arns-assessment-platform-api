package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.exception

import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException
import java.util.UUID

class AnswerNotFoundException(fieldCode: String, aggregateUuid: UUID) :
  AssessmentPlatformException(
    message = "Answer not found",
    developerMessage = "The answer \"$fieldCode\" could not be found on aggregate \"$aggregateUuid\"",
    statusCode = HttpStatus.BAD_REQUEST,
  )
