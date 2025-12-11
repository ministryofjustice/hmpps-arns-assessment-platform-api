package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.config.Clock
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "assessment")
class AssessmentEntity(
  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "uuid")
  val uuid: UUID = UUID.randomUUID(),

  @Column(name = "created_at")
  val createdAt: LocalDateTime = Clock.now(),

  @Column(name = "type", nullable = false)
  val type: String,

  @OneToMany(mappedBy = "assessment", fetch = FetchType.LAZY, orphanRemoval = true, cascade = [CascadeType.ALL])
  val identifiers: MutableList<AssessmentIdentifierEntity> = mutableListOf(),
) {
  fun identifiersMap() = identifiers.associate { it.identifierType to it.identifier }
}
