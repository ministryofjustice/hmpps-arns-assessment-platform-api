package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping

import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.formconfig.FormConfig
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.AnswersProvider
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.OasysSectionMapping
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.SanSectionMapping
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.exception.MappingNotFoundException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MappingProviderTest {
  private lateinit var sut: MappingProvider

  @BeforeTest
  fun setUp() {
    sut = MappingProvider()
  }

  @Nested
  inner class GetOasysSectionMappings {
    private val answersProvider = AnswersProvider(AssessmentAggregate(), FormConfig("v1.0"))

    @ParameterizedTest(name = "should throw an exception for version `{0}`")
    @ValueSource(strings = ["X.Y", ""])
    fun `throws exception when version not found`(version: String) {
      val exception = assertFailsWith<MappingNotFoundException>(
        block = {
          sut.get(version, answersProvider)
        },
      )
      assertContains(exception.message!!, "No SAN->OASys data mapping found for form version $version")
    }

    @Test
    fun `returns section mappings for existing form version`() {
      val result = sut.get("v1.0", answersProvider)
      assertIs<Set<OasysSectionMapping>>(result)
      assertTrue(result.isNotEmpty())
    }

    @Test
    fun `returns fresh section mapping instances on each call`() {
      val first = sut.get("v1.0", answersProvider)
      val second = sut.get("v1.0", answersProvider)
      assertTrue(first.none { a -> second.any { b -> a === b } })
    }
  }

  @Nested
  inner class GetSanSectionMappings {
    @ParameterizedTest(name = "should throw an exception for version `{0}`")
    @ValueSource(strings = ["X.Y", ""])
    fun `throws exception when version not found`(version: String) {
      val exception = assertFailsWith<MappingNotFoundException>(
        block = {
          sut.get(version)
        },
      )
      assertContains(exception.message!!, "No OASys->SAN data mapping found for form version $version")
    }

    @Test
    fun `returns section mappings for existing form version`() {
      val result = sut.get("v2.0")
      assertIs<Set<SanSectionMapping>>(result)
      assertTrue(result.isNotEmpty())
    }
  }
}
