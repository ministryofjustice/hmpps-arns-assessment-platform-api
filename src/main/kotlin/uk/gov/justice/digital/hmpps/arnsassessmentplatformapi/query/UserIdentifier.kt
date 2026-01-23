package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AuthSource
import java.util.UUID

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  JsonSubTypes.Type(value = ExternalUserIdentifier::class, name = "EXTERNAL"),
  JsonSubTypes.Type(value = InternalUserIdentifier::class, name = "INTERNAL"),
)
sealed interface UserIdentifier

data class ExternalUserIdentifier(val identifier: String, val userType: AuthSource) : UserIdentifier

data class InternalUserIdentifier(val uuid: UUID) : UserIdentifier
