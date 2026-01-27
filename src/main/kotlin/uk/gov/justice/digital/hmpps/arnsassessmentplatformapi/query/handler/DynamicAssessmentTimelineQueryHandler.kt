package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler

import org.springframework.data.domain.Page
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentPropertiesUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentRolledBackEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssignedToUserEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemAddedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemPropertiesUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemRemovedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemReorderedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.FormVersionUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentExternalQueryIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentUuidQueryIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.DynamicAssessmentTimelineQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.Events
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.ExternalIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.QueryIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.Timeframe
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UserQueryIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UuidIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.Window
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.DynamicAssessmentTimelineQueryResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.PageInfo
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.QueryResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.TimelineItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.User

@Component
class DynamicAssessmentTimelineQueryHandler(
  private val services: QueryHandlerServiceBundle,
) : QueryHandler<DynamicAssessmentTimelineQuery> {
  override val type = DynamicAssessmentTimelineQuery::class

  fun handleAssessmentTimeline(assessmentIdentifier: AssessmentIdentifier, window: Window): QueryResult {
    val assessmentIdentifier = when (assessmentIdentifier) {
      is ExternalIdentifier -> services.assessmentService.findBy(assessmentIdentifier).uuid
      is UuidIdentifier -> assessmentIdentifier.uuid
    }

    return when (window) {
      is Timeframe -> services.eventService.findAllBetweenByAssessmentUuid(
        assessmentIdentifier,
        window.from,
        window.to,
      ).toTimeLineItems()

      is Events -> services.eventService.findAllPageableByAssessmentUuid(
        assessmentIdentifier,
        window.count,
        window.page,
      ).toTimeLineItemsWithPageInfo()
    }
  }

  fun handleUserTimeline(userIdentifier: UserQueryIdentifier, window: Window) = when (window) {
    is Timeframe -> services.eventService.findAllBetweenByUserUuid(
      userIdentifier.uuid,
      window.from,
      window.to,
    ).toTimeLineItems()

    is Events -> services.eventService.findAllPageableByUserUuid(
      userIdentifier.uuid,
      window.count,
      window.page,
    ).toTimeLineItemsWithPageInfo()
  }

  override fun handle(query: DynamicAssessmentTimelineQuery) = when (query.identifier) {
    is AssessmentExternalQueryIdentifier,
    is AssessmentUuidQueryIdentifier,
    -> handleAssessmentTimeline(query.identifier.toAssessmentIdentifier(), query.window)

    is UserQueryIdentifier -> handleUserTimeline(query.identifier, query.window)
  }

  private fun EventEntity<*>.toTimeLineItem(): TimelineItem {
    val event = data
    return TimelineItem(
      timestamp = createdAt,
      user = User(user.uuid, user.displayName),
      event = event.typeAsString(),
    ).apply {
      when (event) {
        is AssessmentAnswersUpdatedEvent -> {
          this.data["added"] = event.added.keys
          this.data["removed"] = event.removed
        }

        is AssessmentPropertiesUpdatedEvent -> {
          this.data["added"] = event.added.keys
          this.data["removed"] = event.removed
        }

        is AssessmentRolledBackEvent -> this.data["rolledBackTo"] = event.rolledBackTo
        is AssignedToUserEvent -> this.data["assignee"] = event.userUuid
        is CollectionCreatedEvent -> this.data["name"] = event.name
        is CollectionItemAddedEvent -> {
          this.data["index"] = event.index.toString()
          this.data["collection"] = event.collectionName
        }

        is CollectionItemAnswersUpdatedEvent -> {
          this.data["index"] = event.index
          this.data["collection"] = event.collectionName
          this.data["added"] = event.added.keys
          this.data["removed"] = event.removed
        }

        is CollectionItemPropertiesUpdatedEvent -> {
          this.data["index"] = event.index
          this.data["collection"] = event.collectionName
          this.data["added"] = event.added.keys
          this.data["removed"] = event.removed
        }

        is CollectionItemRemovedEvent -> {
          this.data["index"] = event.index
          this.data["collection"] = event.collectionName
        }

        is CollectionItemReorderedEvent -> {
          this.data["index"] = event.index
          this.data["collection"] = event.collectionName
          this.data["to"] = event.index
          this.data["from"] = event.previousIndex
        }

        is FormVersionUpdatedEvent -> this.data["version"] = event.version
        else -> {}
      }
    }
  }

  private fun Event.typeAsString() = this::class.simpleName.toString()

  private fun QueryIdentifier.toAssessmentIdentifier() = when (this) {
    is AssessmentExternalQueryIdentifier -> ExternalIdentifier(identifier, identifierType, assessmentType)
    is AssessmentUuidQueryIdentifier -> UuidIdentifier(uuid)
    else -> throw IllegalArgumentException("Unsupported identifier type ${this::class.simpleName}")
  }

  private fun List<EventEntity<*>>.toTimeLineItems() = DynamicAssessmentTimelineQueryResult(
    timeline = this.map { it.toTimeLineItem() },
  )

  private fun Page<EventEntity<*>>.toTimeLineItemsWithPageInfo() = DynamicAssessmentTimelineQueryResult(
    timeline = this.map { it.toTimeLineItem() }.toList(),
    pageInfo = PageInfo(
      pageNumber = this.number,
      totalPages = this.totalPages,
    ),
  )
}
