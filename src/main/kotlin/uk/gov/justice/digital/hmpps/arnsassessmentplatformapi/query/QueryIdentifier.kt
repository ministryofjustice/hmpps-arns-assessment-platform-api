package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierType
import java.util.UUID

data class UserQueryIdentifier(
  val uuid: UUID,
) : QueryIdentifier

data class AssessmentUuidQueryIdentifier(
  val uuid: UUID,
) : QueryIdentifier

data class AssessmentExternalQueryIdentifier(
  val identifier: String,
  val identifierType: IdentifierType,
  val assessmentType: String,
) : QueryIdentifier

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  JsonSubTypes.Type(value = UserQueryIdentifier::class, name = "USER"),
  JsonSubTypes.Type(value = AssessmentUuidQueryIdentifier::class, name = "ASSESSMENT_UUID"),
  JsonSubTypes.Type(value = AssessmentExternalQueryIdentifier::class, name = "ASSESSMENT_EXTERNAL_ID"),
)
sealed interface QueryIdentifier
