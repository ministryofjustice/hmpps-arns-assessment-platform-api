package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AggregateView
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Collection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.CollectionItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.TimelineItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Value
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.UserDetailsEntity
import java.util.UUID

typealias TimelineView = List<TimelineItem>
typealias CollaboratorsView = Set<UserDetailsEntity>
typealias AnswersView = Map<String, Value>
typealias PropertiesView = Map<String, Value>
typealias CollectionsView = List<Collection>
typealias FormVersionView = String

interface AssessmentAggregateView : AggregateView {
  val formVersion: FormVersionView
  val properties: PropertiesView
  val answers: AnswersView
  val collections: CollectionsView
  val collaborators: CollaboratorsView
  val timeline: TimelineView

  fun getCollection(collectionUuid: UUID): Collection?
  fun getCollectionItem(collectionItemUuid: UUID): CollectionItem?
}
