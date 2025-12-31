package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.Aggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.config.Clock
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "aggregate")
class AggregateEntity<T : Aggregate<T>>(
  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  override val id: Long? = null,

  @Column(name = "uuid")
  override val uuid: UUID = UUID.randomUUID(),

  @Version
  override val version: Long = 0,

  @Column(name = "updated_at")
  override var updatedAt: LocalDateTime = Clock.now(),

  @Column(name = "events_from")
  override val eventsFrom: LocalDateTime = Clock.now(),

  @Column(name = "events_to")
  override var eventsTo: LocalDateTime = Clock.now(),

  @Column(name = "events_applied", nullable = false)
  override var numberOfEventsApplied: Long = 0,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assessment_uuid", referencedColumnName = "uuid", updatable = false, nullable = false)
  override val assessment: AssessmentEntity,

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "data", columnDefinition = "jsonb", nullable = false)
  override val data: T,
) : AggregateEntityView<T> {
  fun clone() = AggregateEntity(
    eventsFrom = this.eventsFrom,
    eventsTo = this.eventsTo,
    assessment = this.assessment,
    data = this.data.clone(),
  )
}
