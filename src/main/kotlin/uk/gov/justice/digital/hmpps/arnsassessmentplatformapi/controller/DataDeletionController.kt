package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.DataDeletionRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.DataDeletionDataResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.DataDeletionResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.DataDeletionService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.UUID

@RestController
@RequestMapping("/data-deletion")
class DataDeletionController(
  private val dataDeletionService: DataDeletionService,
) {
  @RequestMapping(path = ["/{assessmentUuid}"], method = [RequestMethod.GET])
  @Operation(description = "Fetch all events and timeline items for an assessment")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Events/timeline retrieved successfully"),
      ApiResponse(
        responseCode = "400",
        description = "Unable to fetch events",
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
  @PreAuthorize("hasAnyRole('ROLE_AAP__FRONTEND_RW')")
  fun getData(
    @PathVariable assessmentUuid: UUID,
  ): DataDeletionDataResponse = dataDeletionService.getData(assessmentUuid)

  @RequestMapping(path = ["/{assessmentUuid}"], method = [RequestMethod.POST])
  @Operation(description = "Updates events, timeline and aggregates")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "An update was attempted - check the `success` property in the response."),
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
  @PreAuthorize("hasAnyRole('ROLE_AAP__FRONTEND_RW')")
  fun updateData(
    @PathVariable assessmentUuid: UUID,
    @RequestBody request: DataDeletionRequest,
    @AuthenticationPrincipal jwt: Jwt,
  ): DataDeletionResponse = dataDeletionService.updateData(assessmentUuid, request, jwt)
}
