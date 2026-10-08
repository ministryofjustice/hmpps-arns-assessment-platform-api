package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.SQLRestriction
import org.hibernate.type.SqlTypes
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline
import java.time.LocalDateTime
import java.util.UUID

typealias TimelineResolver = (custom: Timeline?) -> TimelineEntity

@Entity
@Table(name = "timeline")
@SQLRestriction("deleted IS FALSE")
class TimelineEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "timeline_sequence_gen")
  @SequenceGenerator(
    name = "timeline_sequence_gen",
    sequenceName = "timeline_sequence",
    allocationSize = 100,
  )
  val id: Long? = null,

  @Column(name = "uuid", nullable = false, updatable = false)
  val uuid: UUID = UUID.randomUUID(),

  @Column(name = "position", nullable = false, updatable = false)
  var position: Int? = null,

  @Column(name = "created_at", nullable = false, updatable = false)
  val createdAt: LocalDateTime,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_details_uuid", referencedColumnName = "uuid", updatable = false, nullable = false)
  val user: UserDetailsEntity,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assessment_uuid", referencedColumnName = "uuid", updatable = false, nullable = false)
  val assessment: AssessmentEntity,

  @Column(name = "event_type", nullable = true, updatable = false)
  val eventType: String? = null,

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "data", columnDefinition = "jsonb", nullable = false, updatable = false)
  val data: Map<String, Any> = emptyMap(),

  @Column(name = "custom_type", updatable = false)
  val customType: String? = null,

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "custom_data", columnDefinition = "jsonb", updatable = false)
  val customData: Map<String, Any>? = null,

  @Column(name = "deleted")
  var deleted: Boolean = false,
) {
  companion object {
    fun resolver(event: EventEntity<*>, data: Map<String, Any>): TimelineResolver = { custom: Timeline? ->
      TimelineEntity(
        createdAt = event.createdAt,
        user = event.user,
        assessment = event.assessment,
        eventType = event.data::class.simpleName,
        data = data,
        customType = custom?.type,
        customData = custom?.data,
      )
    }
  }
}
