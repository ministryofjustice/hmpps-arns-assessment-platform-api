package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
  name = "assessment_draft",
  uniqueConstraints = [
    UniqueConstraint(
      name = "uq_assessment_draft_scope",
      columnNames = ["assessment_uuid", "owner_user_details_uuid", "form_version", "draft_key"],
    ),
  ],
)
class AssessmentDraftEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "assessment_uuid", nullable = false, updatable = false)
  val assessmentUuid: UUID,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_user_details_uuid", referencedColumnName = "uuid", nullable = false, updatable = false)
  val owner: UserDetailsEntity,

  @Column(name = "draft_key", nullable = false, updatable = false)
  val draftKey: String,

  @Column(name = "form_version", nullable = false, updatable = false)
  val formVersion: String,

  @Column(name = "base_aggregate_uuid", nullable = false)
  var baseAggregateUuid: UUID,

  @Column(name = "base_aggregate_version", nullable = false)
  var baseAggregateVersion: Long,

  @Column(name = "revision", nullable = false)
  var revision: Long,

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "data", columnDefinition = "jsonb", nullable = false)
  var data: JsonNode,

  @Column(name = "created_at", nullable = false, updatable = false)
  val createdAt: LocalDateTime,

  @Column(name = "updated_at", nullable = false)
  var updatedAt: LocalDateTime,

  @Column(name = "expires_at", nullable = false)
  var expiresAt: LocalDateTime,
)
