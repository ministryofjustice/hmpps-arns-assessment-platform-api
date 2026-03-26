package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import java.util.UUID

enum class AuthSource {
  OASYS,
  HMPPS_AUTH,
  NOT_SPECIFIED,
}

@Entity
@Table(name = "user_details")
data class UserDetailsEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_details_sequence_gen")
  @SequenceGenerator(
    name = "user_details_sequence_gen",
    sequenceName = "user_details_sequence",
    allocationSize = 100,
  )
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
