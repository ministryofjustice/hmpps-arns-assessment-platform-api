package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.transaction.Transactional
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.CommandRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.CreateAssessmentRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.CreateAssessmentResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.aggregates.AggregateResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.aggregates.AggregateResponseMapperRegistry
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.CreateAssessment
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AggregateService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.CommandBus
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDateTime
import java.util.UUID

@RestController
class AssessmentController(
  private val commandBus: CommandBus,
  private val aggregateService: AggregateService,
  private val assessmentService: AssessmentService,
  private val aggregateResponseMapperRegistry: AggregateResponseMapperRegistry,
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
  @Transactional
  fun executeCommands(
    @RequestBody
    request: CommandRequest,
  ) = commandBus.dispatch(request.commands)

  @RequestMapping(path = ["/assessment/create"], method = [RequestMethod.POST])
  @Operation(description = "Creates an assessment")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Assessment created"),
      ApiResponse(
        responseCode = "400",
        description = "Unable to create assessment",
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
  @Transactional
  fun createAssessment(
    @RequestBody
    request: CreateAssessmentRequest,
  ) = CreateAssessment(request.user)
    .also { command -> commandBus.dispatch(listOf(command)) }
    .let { result -> CreateAssessmentResponse.from(result) }

  @RequestMapping(path = ["/aggregate/{type}/{assessmentUuid}"], method = [RequestMethod.GET])
  @Operation(description = "Fetches the latest aggregate for a given type")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Aggregate found"),
      ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
      ApiResponse(
        responseCode = "404",
        description = "Assessment or aggregate not found",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
      ApiResponse(
        responseCode = "500",
        description = "Unexpected error",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_ARNS_ASSESSMENT_PLATFORM_READ')")
  fun getAggregate(
    @PathVariable type: String,
    @PathVariable assessmentUuid: UUID,
    @RequestParam timestamp: LocalDateTime?,
  ): AggregateResponse {
    val aggregate = assessmentService.findByUuid(assessmentUuid)
      .let { assessment -> aggregateService.fetchOrCreateAggregate(assessment, type, timestamp) }

    return aggregateResponseMapperRegistry.createResponseFrom(aggregate.data)
  }
}
