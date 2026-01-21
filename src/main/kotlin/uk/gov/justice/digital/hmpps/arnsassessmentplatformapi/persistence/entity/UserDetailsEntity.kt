package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

enum class AuthSource {
  OASYS,
  DELIUS,
  NOMIS,
  NOT_SPECIFIED,
}

@Entity
@Table(name = "user_details")
data class UserDetailsEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Long? = null,

  @Column(name = "uuid", nullable = false)
  val uuid: UUID = UUID.randomUUID(),

  @Column(name = "user_id", nullable = false)
  val userId: String,

  @Column(name = "display_name", nullable = false)
  val displayName: String,

  @Column(name = "auth_source", nullable = false)
  @Enumerated(EnumType.STRING)
  val authSource: AuthSource,
)
