package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
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
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.bus.CommandDispatcher
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.CommandsRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.QueriesRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.CommandsResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.QueriesResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.bus.QueryBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.UUID

@RestController
class AssessmentController(
  private val commandDispatcher: CommandDispatcher,
  private val queryBus: QueryBus,
  private val assessmentService: AssessmentService,
) {
  @RequestMapping(path = ["/command"], method = [RequestMethod.POST])
  @Parameter(
    name = "backdateTo",
    description = "Backdate the request to a specific timestamp",
    content = [Content(schema = Schema(type = "string", format = "date-time"))],
    example = "2025-01-01T09:00:00",
    required = false,
    `in` = ParameterIn.QUERY,
  )
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
  @PreAuthorize("hasAnyRole('ROLE_AAP__FRONTEND_RW', 'ROLE_AAP__COORDINATOR_RW')")
  fun executeCommands(
    @RequestBody
    request: CommandsRequest,
  ): CommandsResponse = commandDispatcher.dispatch(request.commands)

  @RequestMapping(path = ["/query"], method = [RequestMethod.POST])
  @Operation(description = "Execute queries on an assessment")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Queries successfully executed"),
      ApiResponse(
        responseCode = "400",
        description = "Unable to process queries",
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
  @PreAuthorize("hasAnyRole('ROLE_AAP__FRONTEND_RW', 'ROLE_AAP__COORDINATOR_RW')")
  fun executeQueries(
    @RequestBody
    request: QueriesRequest,
  ): QueriesResponse = queryBus.dispatch(request.queries)

  @RequestMapping(path = ["/assessment/{assessmentUuid}"], method = [RequestMethod.DELETE])
  @Operation(description = "Deletes an assessment and all related entities")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Assessment deleted"),
      ApiResponse(
        responseCode = "400",
        description = "Unable to process queries",
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
  @PreAuthorize("hasAnyRole('ROLE_AAP__COORDINATOR_RW')")
  fun deleteAssessment(
    @PathVariable("assessmentUuid") assessmentUuid: UUID,
  ) {
    assessmentService.findBy(assessmentUuid)
      .run(assessmentService::delete)
  }
}
