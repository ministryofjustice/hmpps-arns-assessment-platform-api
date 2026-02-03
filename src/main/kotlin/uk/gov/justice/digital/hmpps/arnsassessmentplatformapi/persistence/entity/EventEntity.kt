package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.config.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.GroupEvent
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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_uuid", referencedColumnName = "uuid")
  var parent: EventEntity<GroupEvent>? = null,

  @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
  val children: MutableList<EventEntity<*>> = mutableListOf(),

  @Column(name = "created_at", nullable = false)
  val createdAt: LocalDateTime = Clock.now(),

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_details_uuid", referencedColumnName = "uuid", updatable = false, nullable = false)
  val user: UserDetailsEntity,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assessment_uuid", referencedColumnName = "uuid", updatable = false, nullable = false)
  val assessment: AssessmentEntity,

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "data", columnDefinition = "jsonb", nullable = false)
  val data: E,
)
