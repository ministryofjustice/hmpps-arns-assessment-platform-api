package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.oasys.persistence

import jakarta.persistence.Column
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
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import java.time.LocalDateTime
import java.util.*

enum class CreatedBy {
  CREATED,
  DAILY_EDIT,
  CLONED,
}

@Entity
@Table(name = "oasys_version")
class OasysVersionEntity(
  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "uuid", nullable = false)
  var uuid: UUID? = UUID.randomUUID(),

  @Column(name = "created_at", nullable = false)
  var createdAt: LocalDateTime = Clock.now(),

  @Enumerated(EnumType.STRING)
  @Column(name = "created_by", nullable = false)
  val createdBy: CreatedBy,

  @Column(name = "updated_at", nullable = false)
  var updatedAt: LocalDateTime = Clock.now(),

  @Column(name = "version", nullable = false)
  var version: Long = 0,

  @Column(name = "status", nullable = false, length = Integer.MAX_VALUE)
  var status: String? = null,

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "last_event", nullable = false, updatable = true, referencedColumnName = "uuid")
  var lastEvent: EventEntity<*>,

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "assessment_uuid", nullable = false, referencedColumnName = "uuid")
  var assessment: AssessmentEntity? = null,
)
