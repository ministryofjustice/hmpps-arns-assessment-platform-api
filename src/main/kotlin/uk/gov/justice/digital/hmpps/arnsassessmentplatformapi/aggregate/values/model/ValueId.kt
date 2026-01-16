package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.values.model

import java.util.UUID

enum class ValueIdType {
  ASSESSMENT,
  COLLECTION,
}

data class ValueId(
  val id: String,
  val type: ValueIdType,
) {
  companion object {
    fun of(code: String) = ValueId(code, ValueIdType.ASSESSMENT)
    fun of(code: String, collectionItemUuid: UUID) = ValueId("$collectionItemUuid:$code", ValueIdType.COLLECTION)
  }
}
