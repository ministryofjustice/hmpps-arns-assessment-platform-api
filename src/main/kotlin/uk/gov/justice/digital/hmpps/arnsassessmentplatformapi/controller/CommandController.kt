package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller

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
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.CommandRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.ErrorResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.CommandService

@RestController
class CommandController(
  private val commandService: CommandService,
) {

  @RequestMapping(path = ["/command"], method = [RequestMethod.POST])
  @Operation(description = "Execute commands on an assessment")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Commands successfully executed"),
      ApiResponse(
        responseCode = "400",
        description = "Unable to process commands",
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
  @PreAuthorize("hasAnyRole('ROLE_ARNS_ASSESSMENT_PLATFORM_WRITE')")
  fun executeCommands(
    @RequestBody
    request: CommandRequest,
  ) = commandService.process(request)
}
