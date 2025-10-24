package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.CollectionEntity
import java.util.UUID

@Repository
interface CollectionRepository : JpaRepository<CollectionEntity, Long> {

  fun findByUuid(uuid: UUID): CollectionEntity?

  @Query(
    """
        SELECT c FROM CollectionEntity c
        WHERE c.leftBound >= :lft AND c.rightBound <= :rgt
        ORDER BY c.leftBound ASC
    """,
  )
  fun findSubtree(@Param("lft") lft: Long, @Param("rgt") rgt: Long): List<CollectionEntity>

  @Modifying
  @Query("UPDATE CollectionEntity c SET c.rightBound = c.rightBound + :delta WHERE c.rightBound >= :fromRgt")
  fun shiftRightBoundsFrom(@Param("fromRgt") fromRgt: Long, @Param("delta") delta: Long): Int

  @Modifying
  @Query("UPDATE CollectionEntity c SET c.leftBound = c.leftBound + :delta WHERE c.leftBound > :fromRgt")
  fun shiftLeftBoundsFrom(@Param("fromRgt") fromRgt: Long, @Param("delta") delta: Long): Int

  @Modifying
  @Query(
    """
        UPDATE CollectionEntity c 
        SET c.leftBound = c.leftBound - :delta 
        WHERE c.leftBound > :after
    """,
  )
  fun closeGapLeft(@Param("after") after: Long, @Param("delta") delta: Long): Int

  @Modifying
  @Query(
    """
        UPDATE CollectionEntity c 
        SET c.rightBound = c.rightBound - :delta 
        WHERE c.rightBound > :after
    """,
  )
  fun closeGapRight(@Param("after") after: Long, @Param("delta") delta: Long): Int

  @Modifying
  @Query("DELETE FROM CollectionEntity c WHERE c.leftBound BETWEEN :lft AND :rgt")
  fun deleteSubtreeInternal(@Param("lft") lft: Long, @Param("rgt") rgt: Long): Int
}
