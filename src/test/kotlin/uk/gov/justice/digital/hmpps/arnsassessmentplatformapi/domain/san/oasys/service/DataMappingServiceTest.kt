package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.service

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Nested
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.formconfig.FormConfig
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Field
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.MappingProvider
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.AnswersProvider
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.SectionMapping
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.formconfig.Field as FormField

class DataMappingServiceTest {
  private val mockMappingProvider: MappingProvider = mockk()
  private val mockSectionMapping: SectionMapping = mockk()
  private val testConfig = FormConfig(
    "1.0",
    mapOf(Field.TEST_FIELD.lower to FormField(Field.TEST_FIELD.lower)),
  )

  @BeforeTest
  fun setUp() {
    clearAllMocks()
  }

  @Nested
  inner class GetOasysEquivalent {
    @Test
    fun `returns empty result`() {
      every { mockSectionMapping.map(any<AnswersProvider>()) } returns emptyMap()
      every { mockMappingProvider.get(match { formVersion -> formVersion == "1.0" }) } returns setOf(
        mockSectionMapping,
      )

      val sut = DataMappingService(mockMappingProvider)
      val result = sut.getOasysEquivalent(AssessmentAggregate(), testConfig)

      assertEquals(emptyMap(), result)
    }

    @Test
    fun `returns non-empty result comprising of multiple section mappings`() {
      val mockSectionMappingTwo: SectionMapping = mockk()

      every { mockSectionMapping.map(any<AnswersProvider>()) } returns mapOf("oasys-key-1" to "val-1")
      every { mockSectionMappingTwo.map(any<AnswersProvider>()) } returns mapOf(
        "oasys-key-2" to listOf(
          "val-2",
          "val-3",
        ),
      )
      every { mockMappingProvider.get(match { formVersion -> formVersion == "1.0" }) } returns setOf(
        mockSectionMapping,
        mockSectionMappingTwo,
      )

      val sut = DataMappingService(mockMappingProvider)

      val assessment = AssessmentAggregate().apply {
        formVersion = "1.0"
        answers.putAll(mapOf(Field.TEST_FIELD.lower to SingleValue(value = "all good")))
      }

      val result = sut.getOasysEquivalent(assessment, testConfig)

      verify(exactly = 1) {
        mockSectionMapping.map(withArg { assertEquals("all good", it.answer(Field.TEST_FIELD).value) })
        mockSectionMappingTwo.map(withArg { assertEquals("all good", it.answer(Field.TEST_FIELD).value) })
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
}
