package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AggregateView
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Collection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.CollectionItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Value
import java.util.UUID

typealias CollaboratorsView = Set<UUID>
typealias AnswersView = Map<String, Value>
typealias PropertiesView = Map<String, Value>
typealias CollectionsView = List<Collection>
typealias FlagsView = List<String>
typealias FormVersionView = String

interface AssessmentAggregateView : AggregateView {
  val formVersion: FormVersionView
  val assignedUser: UUID?
  val properties: PropertiesView
  val answers: AnswersView
  val collections: CollectionsView
  val collaborators: CollaboratorsView
  val flags: FlagsView

  fun getCollection(collectionUuid: UUID): Collection?
  fun getCollectionWithItem(collectionItemUuid: UUID): Collection?
  fun getCollectionItem(collectionItemUuid: UUID): CollectionItem?
}
