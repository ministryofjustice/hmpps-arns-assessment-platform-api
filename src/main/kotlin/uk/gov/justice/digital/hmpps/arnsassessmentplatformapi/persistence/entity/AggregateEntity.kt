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
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate.Aggregate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.apply

@Entity
@Table(name = "aggregate")
class AggregateEntity(
  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "uuid")
  val uuid: UUID = UUID.randomUUID(),

  @Column(name = "updated_at")
  var updatedAt: LocalDateTime = LocalDateTime.now(),

  @Column(name = "events_from")
  val eventsFrom: LocalDateTime = LocalDateTime.now(),

  @Column(name = "events_to")
  var eventsTo: LocalDateTime = LocalDateTime.now(),

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assessment_uuid", referencedColumnName = "uuid", updatable = false, nullable = false)
  val assessment: AssessmentEntity,

  @Type(JsonType::class)
  @Column(name = "data", nullable = false)
  val data: Aggregate,
) {
  fun apply(event: EventEntity): Boolean {
    eventsTo = event.createdAt
    updatedAt = LocalDateTime.now()
    return data.apply(event)
  }

  fun clone(): AggregateEntity = AggregateEntity(
    eventsFrom = this.eventsFrom,
    eventsTo = this.eventsTo,
    assessment = this.assessment,
    data = this.data.clone(),
  )

  companion object {
    fun init(assessment: AssessmentEntity, data: Aggregate, events: List<EventEntity> = emptyList()) = AggregateEntity(
      assessment = assessment,
      data = data,
      eventsFrom = events.minByOrNull { it.createdAt }?.createdAt ?: assessment.createdAt,
    ).apply { events.forEach { data.apply(it) } }

    fun getDefault(assessment: AssessmentEntity, data: Aggregate) = AggregateEntity(
      assessment = assessment,
      data = data,
      eventsFrom = assessment.createdAt,
      eventsTo = assessment.createdAt,
      updatedAt = LocalDateTime.now(),
    )
  }
}
