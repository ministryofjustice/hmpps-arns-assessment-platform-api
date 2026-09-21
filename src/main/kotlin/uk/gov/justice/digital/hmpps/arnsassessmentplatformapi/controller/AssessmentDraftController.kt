package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.AssessmentDraftRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.AssessmentDraftQueryRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.AssessmentDraftResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentDraftService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.UUID

@RestController
@RequestMapping("/assessment/{assessmentUuid}/draft")
class AssessmentDraftController(
  private val assessmentDraftService: AssessmentDraftService,
) {
  @RequestMapping(method = [RequestMethod.PUT])
  @Operation(description = "Create or update the authenticated editor's autosave draft")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Draft saved"),
      ApiResponse(responseCode = "409", description = "A newer draft is already stored"),
      ApiResponse(
        responseCode = "400",
        description = "Invalid draft",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_AAP__FRONTEND_RW', 'ROLE_AAP__COORDINATOR_RW', 'ROLE_SENTENCE_PLAN_WRITE')")
  fun save(
    @PathVariable assessmentUuid: UUID,
    @RequestBody request: AssessmentDraftRequest,
  ): AssessmentDraftResponse = assessmentDraftService.save(assessmentUuid, request)

  @RequestMapping(path = ["/query"], method = [RequestMethod.POST])
  @Operation(description = "Get the current editor's unexpired autosave drafts")
  @PreAuthorize("hasAnyRole('ROLE_AAP__FRONTEND_RW', 'ROLE_AAP__COORDINATOR_RW', 'ROLE_SENTENCE_PLAN_WRITE')")
  fun findAll(
    @PathVariable assessmentUuid: UUID,
    @RequestBody request: AssessmentDraftQueryRequest,
  ): List<AssessmentDraftResponse> = assessmentDraftService.findAll(assessmentUuid, request.user)
}
