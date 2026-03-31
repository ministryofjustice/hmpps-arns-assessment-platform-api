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
import org.hibernate.type.SqlTypes
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "event")
class EventEntity<E : Event>(
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "event_sequence_gen")
  @SequenceGenerator(
    name = "event_sequence_gen",
    sequenceName = "event_sequence",
    allocationSize = 100,
  )
  val id: Long? = null,

  @Column(name = "uuid", nullable = false, updatable = false)
  val uuid: UUID = UUID.randomUUID(),

  @Column(name = "created_at", nullable = false, updatable = false)
  val createdAt: LocalDateTime,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_details_uuid", referencedColumnName = "uuid", updatable = false, nullable = false)
  val user: UserDetailsEntity,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assessment_uuid", referencedColumnName = "uuid", updatable = false, nullable = false)
  val assessment: AssessmentEntity,

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "data", columnDefinition = "jsonb", updatable = false, nullable = false)
  val data: E,
)
