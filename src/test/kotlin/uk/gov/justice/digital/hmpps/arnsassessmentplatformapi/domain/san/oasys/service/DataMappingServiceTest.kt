package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.service

import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Nested
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.formconfig.FormConfig
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Field
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.MappingProvider
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.AnswersProvider
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.OasysSectionMapping
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.SanSectionMapping
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.formconfig.Field as FormField

class DataMappingServiceTest {
  private val mockMappingProvider: MappingProvider = mockk()

  @BeforeTest
  fun setUp() {
    clearAllMocks()
  }

  @Nested
  inner class GetOasysEquivalent {
    private val mockOasysSectionMapping: OasysSectionMapping = mockk()
    private val testConfig = FormConfig(
      "1.0",
      mapOf(Field.TEST_FIELD.lower to FormField(Field.TEST_FIELD.lower)),
    )

    @Test
    fun `returns empty result`() {
      every { mockOasysSectionMapping.map() } returns emptyMap()
      every {
        mockMappingProvider.get(match { formVersion -> formVersion == "1.0" }, any<AnswersProvider>())
      } returns setOf(
        mockOasysSectionMapping,
      )

      val sut = DataMappingService(mockMappingProvider)
      val result = sut.getOasysEquivalent(AssessmentAggregate(), testConfig)

      assertEquals(emptyMap(), result)
    }

    @Test
    fun `returns non-empty result comprising of multiple section mappings`() {
      val mockOasysSectionMappingTwo: OasysSectionMapping = mockk()

      val answersProviderSlot = slot<AnswersProvider>()

      every { mockOasysSectionMapping.map() } returns mapOf("oasys-key-1" to "val-1")
      every { mockOasysSectionMappingTwo.map() } returns mapOf(
        "oasys-key-2" to listOf(
          "val-2",
          "val-3",
        ),
      )
      every {
        mockMappingProvider.get(
          match { formVersion -> formVersion == "1.0" },
          capture(answersProviderSlot),
        )
      } returns setOf(
        mockOasysSectionMapping,
        mockOasysSectionMappingTwo,
      )

      val sut = DataMappingService(mockMappingProvider)

      val assessment = AssessmentAggregate().apply {
        formVersion = "1.0"
        answers.putAll(mapOf(Field.TEST_FIELD.lower to SingleValue(value = "all good")))
      }

      val result = sut.getOasysEquivalent(assessment, testConfig)

      assertEquals("all good", answersProviderSlot.captured.answer(Field.TEST_FIELD).value)

      verify(exactly = 1) {
        mockOasysSectionMapping.map()
        mockOasysSectionMappingTwo.map()
      }

      assertEquals(
        mapOf(
          "oasys-key-1" to "val-1",
          "oasys-key-2" to listOf("val-2", "val-3"),
        ),
        result,
      )
    }
  }

  @Nested
  inner class GetSanEquivalent {
    private val assessmentAggregate = AssessmentAggregate().apply { formVersion = "v1.0" }
    private val oasysData: OasysEquivalent = mapOf("oasys-key-1" to "val-1")
    private val mockSanSectionMapping: SanSectionMapping = mockk()

    @Test
    fun `returns empty result`() {
      every {
        mockSanSectionMapping.map(oasysData, any())
      } just Runs

      every {
        mockMappingProvider.get(match { formVersion -> formVersion == "v1.0" })
      } returns setOf(
        mockSanSectionMapping,
      )

      val sut = DataMappingService(mockMappingProvider)
      val result = sut.getSanEquivalent(assessmentAggregate, oasysData)

      assertEquals(emptyMap(), result.answers)
      assertEquals(emptyMap(), result.properties)
      assertEquals(emptyList(), result.collections)
      assertEquals(emptyList(), result.flags)

      verify(exactly = 1) { mockSanSectionMapping.map(oasysData, any()) }
    }

    @Test
    fun `returns an aggregate mutated by multiple section mappings`() {
      val mockSanSectionMappingTwo: SanSectionMapping = mockk()

      val mappingsAggregateSlot1 = slot<AssessmentAggregate>()
      val mappingsAggregateSlot2 = slot<AssessmentAggregate>()

      every {
        mockSanSectionMapping.map(oasysData, capture(mappingsAggregateSlot1))
      } just Runs

      every {
        mockSanSectionMappingTwo.map(oasysData, capture(mappingsAggregateSlot2))
      } just Runs

      every {
        mockMappingProvider.get(match { formVersion -> formVersion == "v1.0" })
      } returns setOf(
        mockSanSectionMapping,
        mockSanSectionMappingTwo,
      )

      val sut = DataMappingService(mockMappingProvider)

      val result = sut.getSanEquivalent(assessmentAggregate, oasysData)

      verify(exactly = 1) {
        mockSanSectionMapping.map(oasysData, any())
        mockSanSectionMappingTwo.map(oasysData, any())
      }

      assertSame(result, mappingsAggregateSlot1.captured)
      assertSame(mappingsAggregateSlot1.captured, mappingsAggregateSlot2.captured)
    }
  }
}
