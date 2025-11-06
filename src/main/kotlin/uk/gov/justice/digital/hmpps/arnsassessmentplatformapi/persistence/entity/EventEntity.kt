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
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "event")
class EventEntity<E : Event>(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  var id: Long? = null,

  @Column(name = "uuid", nullable = false)
  var uuid: UUID = UUID.randomUUID(),

  @Column(name = "created_at", nullable = false)
  val createdAt: LocalDateTime = LocalDateTime.now(),

  @Type(JsonType::class)
  @Column(name = "user_details", nullable = false)
  val user: User,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assessment_uuid", referencedColumnName = "uuid", updatable = false, nullable = false)
  val assessment: AssessmentEntity,

  @Type(JsonType::class)
  @Column(name = "data", nullable = false)
  val data: E,
)
