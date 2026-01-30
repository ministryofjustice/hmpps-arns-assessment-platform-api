package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.criteria

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.FieldSource
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity_
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity_
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.UserDetailsEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.UserDetailsEntity_
import java.time.LocalDateTime
import java.util.UUID

@DisplayName("TimelineCriteria Specification tests")
class TimelineCriteriaTest {
  private val root = mockk<Root<TimelineEntity>>(relaxed = true)
  private val query = mockk<CriteriaQuery<*>>(relaxed = true)
  private val builder = mockk<CriteriaBuilder>(relaxed = true)

  private val assessmentPath = mockk<Path<AssessmentEntity>>(relaxed = true)
  private val assessmentUuidPath = mockk<Path<UUID>>(relaxed = true)

  private val userPath = mockk<Path<UserDetailsEntity>>(relaxed = true)
  private val userUuidPath = mockk<Path<UUID>>(relaxed = true)

  private val createdAtPath = mockk<Path<LocalDateTime>>(relaxed = true)
  private val customTypePath = mockk<Path<String>>(relaxed = true)
  private val eventTypePath = mockk<Path<String>>(relaxed = true)

  @BeforeEach
  fun setupCommonMocks() {
    clearAllMocks()

    every { root.get(TimelineEntity_.assessment) } returns assessmentPath
    every { assessmentPath.get(AssessmentEntity_.uuid) } returns assessmentUuidPath

    every { root.get(TimelineEntity_.user) } returns userPath
    every { userPath.get(UserDetailsEntity_.uuid) } returns userUuidPath

    every { root.get(TimelineEntity_.createdAt) } returns createdAtPath
  }

  // ---------- DSL-style helpers ----------

  private fun givenAssessment(uuid: UUID) {
    every { builder.equal(assessmentUuidPath, uuid) } returns mockk()
  }

  private fun givenUser(uuid: UUID) {
    every { builder.equal(userUuidPath, uuid) } returns mockk()
  }

  private fun givenFrom(time: LocalDateTime) {
    every { builder.greaterThanOrEqualTo(createdAtPath, time) } returns mockk()
  }

  private fun givenTo(time: LocalDateTime) {
    every { builder.lessThanOrEqualTo(createdAtPath, time) } returns mockk()
  }

  private fun givenEventTypes(types: Set<String>) {
    every { eventTypePath.`in`(types) } returns mockk()
    every { builder.and(any<Predicate>()) } returns mockk()
    every { builder.not(any()) } returns mockk()
  }

  private fun givenCustomTypes(types: Set<String>) {
    every { customTypePath.`in`(types) } returns mockk()
    every { builder.and(any<Predicate>()) } returns mockk()
    every { builder.not(any()) } returns mockk()
  }

  private fun TimelineCriteria.buildSpec(): Specification<TimelineEntity> = this.getSpecification()

  // ---------- Nested Test Structure ----------

  @Nested
  inner class Validation {

    @Test
    fun `throws when neither assessmentUuid nor userUuid provided`() {
      val ex = assertThrows(RuntimeException::class.java) {
        TimelineCriteria().getSpecification()
      }
      assertEquals("Must specify at least one of assessmentUuid or userUuid", ex.message)
    }
  }

  @Nested
  inner class AssessmentFilters {

    @Test
    fun `adds assessment uuid predicate`() {
      val uuid = UUID.randomUUID()
      givenAssessment(uuid)

      val spec = TimelineCriteria(assessmentUuid = uuid).buildSpec()
      spec.toPredicate(root, query, builder)

      val pathSlot = slot<Expression<UUID>>()
      verify {
        builder.equal(capture(pathSlot), uuid)
      }
    }
  }

  @Nested
  inner class UserFilters {

    @Test
    fun `adds user uuid predicate`() {
      val uuid = UUID.randomUUID()
      givenUser(uuid)

      val spec = TimelineCriteria(userUuid = uuid).buildSpec()
      spec.toPredicate(root, query, builder)

      val pathSlot = slot<Expression<UUID>>()
      verify {
        builder.equal(capture(pathSlot), uuid)
      }
    }
  }

  @Nested
  inner class TimeFilters {

    @Test
    fun `adds from timestamp predicate`() {
      val uuid = UUID.randomUUID()
      val from = LocalDateTime.now().minusDays(1)
      givenAssessment(uuid)
      givenFrom(from)

      val spec = TimelineCriteria(assessmentUuid = uuid, from = from).buildSpec()
      spec.toPredicate(root, query, builder)

      val pathSlot = slot<Expression<LocalDateTime>>()
      verify { builder.greaterThanOrEqualTo(capture(pathSlot), from) }
    }

    @Test
    fun `adds to timestamp predicate`() {
      val uuid = UUID.randomUUID()
      val to = LocalDateTime.now()
      givenAssessment(uuid)
      givenTo(to)

      val spec = TimelineCriteria(assessmentUuid = uuid, to = to).buildSpec()
      spec.toPredicate(root, query, builder)

      val pathSlot = slot<Expression<LocalDateTime>>()
      verify { builder.lessThanOrEqualTo(capture(pathSlot), to) }
    }
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  inner class TypeFilters {

    val emptyAndNonEmptySets = listOf(
      emptySet(),
      setOf("A"),
      setOf("A", "B"),
    )

    @ParameterizedTest(name = "includeEventTypes = {0}")
    @FieldSource("emptyAndNonEmptySets")
    fun `include event types predicate conditional`(types: Set<String>) {
      val uuid = UUID.randomUUID()
      givenAssessment(uuid)
      every { root.get(TimelineEntity_.eventType) } returns eventTypePath
      if (types.isNotEmpty()) givenEventTypes(types)

      val spec = TimelineCriteria(assessmentUuid = uuid, includeEventTypes = types).buildSpec()
      spec.toPredicate(root, query, builder)

      if (types.isEmpty()) {
        verify(exactly = 0) { eventTypePath.`in`(any<Collection<String>>()) }
      } else {
        val slot = slot<Collection<String>>()
        verify { eventTypePath.`in`(capture(slot)) }
        assertEquals(types, slot.captured.toSet())
      }
    }

    @ParameterizedTest(name = "excludeEventTypes = {0}")
    @FieldSource("emptyAndNonEmptySets")
    fun `exclude event types predicate conditional`(types: Set<String>) {
      val uuid = UUID.randomUUID()
      givenAssessment(uuid)
      every { root.get(TimelineEntity_.eventType) } returns eventTypePath
      if (types.isNotEmpty()) givenEventTypes(types)

      val spec = TimelineCriteria(assessmentUuid = uuid, excludeEventTypes = types).buildSpec()
      spec.toPredicate(root, query, builder)

      if (types.isEmpty()) {
        verify(exactly = 0) { builder.not(any()) }
      } else {
        verify { builder.not(any()) }
      }
    }

    @ParameterizedTest(name = "includeCustomTypes = {0}")
    @FieldSource("emptyAndNonEmptySets")
    fun `include custom types predicate conditional`(types: Set<String>) {
      val uuid = UUID.randomUUID()
      givenAssessment(uuid)
      every { root.get(TimelineEntity_.customType) } returns customTypePath
      if (types.isNotEmpty()) givenCustomTypes(types)

      val spec = TimelineCriteria(assessmentUuid = uuid, includeCustomTypes = types).buildSpec()
      spec.toPredicate(root, query, builder)

      if (types.isEmpty()) {
        verify(exactly = 0) { customTypePath.`in`(any<Collection<String>>()) }
      } else {
        val slot = slot<Collection<String>>()
        verify { customTypePath.`in`(capture(slot)) }
        assertEquals(types, slot.captured.toSet())
      }
    }

    @ParameterizedTest(name = "excludeCustomTypes = {0}")
    @FieldSource("emptyAndNonEmptySets")
    fun `exclude custom types predicate conditional`(types: Set<String>) {
      val uuid = UUID.randomUUID()
      givenAssessment(uuid)
      every { root.get(TimelineEntity_.customType) } returns customTypePath
      if (types.isNotEmpty()) givenCustomTypes(types)

      val spec = TimelineCriteria(assessmentUuid = uuid, excludeCustomTypes = types).buildSpec()
      spec.toPredicate(root, query, builder)

      if (types.isEmpty()) {
        verify(exactly = 0) { builder.not(any()) }
      } else {
        verify { builder.not(any()) }
      }
    }
  }
}
