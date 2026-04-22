package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.Aggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Collection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Value
import java.util.UUID
import kotlin.collections.mutableListOf

typealias Collaborators = MutableSet<UUID>
typealias Answers = MutableMap<String, Value>
typealias Properties = MutableMap<String, Value>
typealias Collections = MutableList<Collection>
typealias Flags = MutableList<String>
typealias FormVersion = String

class AssessmentAggregate :
  Aggregate<AssessmentAggregate>,
  AssessmentAggregateView {
  override var formVersion: FormVersion = ""
  override var assignedUser: UUID? = null

  override val properties: Properties = mutableMapOf()
  override val answers: Answers = mutableMapOf()
  override val collections: Collections = mutableListOf()
  override val collaborators: Collaborators = mutableSetOf()
  override val flags: Flags = mutableListOf()

  override fun clone() = AssessmentAggregate().also { clone ->
    clone.properties.putAll(properties)
    clone.answers.putAll(answers)
    clone.collections.addAll(collections)
    clone.collaborators.addAll(collaborators)
    clone.flags.addAll(flags)
    clone.formVersion = formVersion
    clone.assignedUser = assignedUser
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is AssessmentAggregate) return false
    return formVersion == other.formVersion &&
      assignedUser == other.assignedUser &&
      properties == other.properties &&
      answers == other.answers &&
      collections == other.collections &&
      collaborators == other.collaborators &&
      flags == other.flags
  }

  override fun hashCode(): Int {
    var result = formVersion.hashCode()
    result = 31 * result + (assignedUser?.hashCode() ?: 0)
    result = 31 * result + properties.hashCode()
    result = 31 * result + answers.hashCode()
    result = 31 * result + collections.hashCode()
    result = 31 * result + collaborators.hashCode()
    result = 31 * result + flags.hashCode()
    return result
  }

  override fun getCollection(collectionUuid: UUID) = collections.firstOrNull { it.uuid == collectionUuid }
    ?: collections.firstNotNullOfOrNull { collection ->
      collection.items.firstNotNullOfOrNull {
        it.findCollection(
          collectionUuid,
        )
      }
    }

  override fun getCollectionWithItem(collectionItemUuid: UUID): Collection? = collections.firstNotNullOfOrNull { collection ->
    collection.items.firstNotNullOfOrNull { item ->
      if (item.uuid == collectionItemUuid) {
        collection
      } else {
        item.findCollectionWithItem(collectionItemUuid)
      }
    }
  }

  override fun getCollectionItem(collectionItemUuid: UUID) = collections.firstNotNullOfOrNull { it.findItem(collectionItemUuid) }
}
