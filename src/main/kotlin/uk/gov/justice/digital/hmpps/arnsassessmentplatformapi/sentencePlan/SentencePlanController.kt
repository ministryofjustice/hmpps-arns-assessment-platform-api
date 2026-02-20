package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.sentencePlan

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.sentencePlan.requests.NewPeriodOfSupervisionRequest
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
class SentencePlanController(
        private val sentencePlanService: SentencePlanService

) {
  @RequestMapping(path = ["/plan/start-new-period-of-supervision"], method = [RequestMethod.POST])
  @Operation(description = "Starts a new period of supervision for a Sentence Plan, removes Active and Future Goals")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Started new period of supervision"),
      ApiResponse(
        responseCode = "400",
        description = "The Assessment is not a Sentence Plan",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
      ApiResponse(
        responseCode = "404",
        description = "Assessment not found",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
      ApiResponse(
        responseCode = "500",
        description = "Unexpected error",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
    ],
  )
  @PreAuthorize("hasAnyAuthority('ROLE_AAP__FRONTEND_RW','ROLE_AAP__COORDINATOR_RW')")
  fun newPeriodOfSupervision(@RequestBody request: NewPeriodOfSupervisionRequest) {
    sentencePlanService.newPeriodOfSupervision(request.assessmentUuid, request.user)
  }
}
