package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.controller

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
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.controller.requests.StartNewPathwayRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.service.StrengthsAndNeedsService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
class StrengthsAndNeedsController(
  private val strengthsAndNeedsService: StrengthsAndNeedsService,
) {
  @RequestMapping(path = ["/san/start-new-pathway"], method = [RequestMethod.POST])
  @Operation(description = "Starts a new pathway for a Strengths and Needs Assessment, removes all existing answers")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Started new pathway"),
      ApiResponse(
        responseCode = "400",
        description = "The Assessment is not a Strengths and Needs Assessment, or previous and new pathway are the same",
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
  fun startNewPathway(@RequestBody request: StartNewPathwayRequest) {
    strengthsAndNeedsService.startNewPathway(
      assessmentUuid = request.assessmentUuid,
      userDetails = request.user,
      previousPathway = request.previousPathway,
      newPathway = request.newPathway,
    )
  }
}
