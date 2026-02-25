package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.config.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.ExternalIdentifier
import java.time.LocalDateTime
import java.util.UUID

enum class IdentifierType {
  CRN,
  PRN,
  NOMIS_ID,
}

@Embeddable
data class IdentifierPair(
  @Column(name = "identifier_type")
  @Enumerated(EnumType.STRING)
  val type: IdentifierType,

  @Column(name = "identifier")
  val id: String,
)

@Entity
@Table(name = "assessment_identifier")
class AssessmentIdentifierEntity(
  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "uuid")
  val uuid: UUID = UUID.randomUUID(),

  @Column(name = "created_at")
  val createdAt: LocalDateTime = Clock.now(),

  @Embedded
  val externalIdentifier: IdentifierPair,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assessment_uuid", referencedColumnName = "uuid", updatable = false, nullable = false)
  val assessment: AssessmentEntity,
) {
  fun toIdentifier() = ExternalIdentifier(
    identifier = externalIdentifier.id,
    identifierType = externalIdentifier.type,
    assessmentType = assessment.type,
  )
}
