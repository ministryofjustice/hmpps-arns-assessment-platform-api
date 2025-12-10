package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierType
import java.util.UUID

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  JsonSubTypes.Type(value = ExternalIdentifier::class, name = "EXTERNAL"),
  JsonSubTypes.Type(value = UuidIdentifier::class, name = "UUID"),
)
sealed interface AssessmentIdentifier

class ExternalIdentifier(val identifier: String, val identifierType: IdentifierType, val assessmentType: String) : AssessmentIdentifier

class UuidIdentifier(val uuid: UUID) : AssessmentIdentifier
