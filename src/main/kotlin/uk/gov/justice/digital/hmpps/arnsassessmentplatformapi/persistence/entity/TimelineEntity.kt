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
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Command
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "timeline")
class TimelineEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  var id: Long? = null,

  @Column(name = "uuid", nullable = false)
  var uuid: UUID = UUID.randomUUID(),

  @Column(name = "created_at", nullable = false)
  val createdAt: LocalDateTime,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_details_uuid", referencedColumnName = "uuid", updatable = false, nullable = false)
  val user: UserDetailsEntity,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assessment_uuid", referencedColumnName = "uuid", updatable = false, nullable = false)
  val assessment: AssessmentEntity,

  @Column(name = "event_type", nullable = false)
  var eventType: String,

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "data", columnDefinition = "jsonb", nullable = false)
  val data: Map<String, Any> = emptyMap(),

  @Column(name = "custom_type")
  var customType: String? = null,

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "custom_data", columnDefinition = "jsonb")
  val customData: Map<String, Any>? = null,
) {
  companion object {
    fun from(command: Command, event: EventEntity<*>, data: Map<String, Any>) = TimelineEntity(
      createdAt = event.createdAt,
      user = event.user,
      assessment = event.assessment,
      eventType = event.data::class.simpleName ?: "Unknown",
      data = data,
      customType = command.timeline?.type,
      customData = command.timeline?.data,
    )
  }
}
