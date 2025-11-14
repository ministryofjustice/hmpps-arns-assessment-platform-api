package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity

import com.vladmihalcea.hibernate.type.json.JsonType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Type
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
  val id: Long? = null,

  @Column(name = "uuid")
  val uuid: UUID = UUID.randomUUID(),

  @Column(name = "updated_at")
  var updatedAt: LocalDateTime = Clock.now(),

  @Column(name = "events_from")
  val eventsFrom: LocalDateTime = Clock.now(),

  @Column(name = "events_to")
  var eventsTo: LocalDateTime = Clock.now(),

  @Column(name = "events_applied", nullable = false)
  var numberOfEventsApplied: Long = 0,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assessment_uuid", referencedColumnName = "uuid", updatable = false, nullable = false)
  val assessment: AssessmentEntity,

  @Type(JsonType::class)
  @Column(name = "data", nullable = false)
  val data: T,
) {
  fun clone() = AggregateEntity(
    eventsFrom = this.eventsFrom,
    eventsTo = this.eventsTo,
    assessment = this.assessment,
    data = this.data.clone(),
  )
}
