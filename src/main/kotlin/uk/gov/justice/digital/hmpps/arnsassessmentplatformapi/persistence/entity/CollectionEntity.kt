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
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "collection")
class CollectionEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Long? = null,

  @Column(name = "uuid", nullable = false, updatable = false)
  var uuid: UUID = UUID.randomUUID(),

  @OneToMany(mappedBy = "parent", cascade = [], orphanRemoval = false, fetch = FetchType.LAZY)
  var children: MutableSet<CollectionEntity> = mutableSetOf(),

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_uuid", referencedColumnName = "uuid")
  var parent: CollectionEntity? = null,

  @Column(name = "root_uuid", nullable = false)
  var rootUuid: UUID = uuid,

  @Column(name = "lft")
  var leftBound: Long? = 1,

  @Column(name = "rgt")
  var rightBound: Long? = 2,

  @Column(name = "depth", nullable = false)
  var depth: Int = 0,

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  var createdAt: LocalDateTime = LocalDateTime.now(),
) {
  override fun equals(other: Any?) = this === other || (other is CollectionEntity && uuid == other.uuid)

  override fun hashCode(): Int = uuid.hashCode()

  @PrePersist
  fun assignTreeUuid() {
    rootUuid = parent?.rootUuid ?: uuid
  }
}
