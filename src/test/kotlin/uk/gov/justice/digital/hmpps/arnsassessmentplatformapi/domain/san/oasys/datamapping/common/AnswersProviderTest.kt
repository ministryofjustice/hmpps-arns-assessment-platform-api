package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.formconfig.FieldType
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.formconfig.FormConfig
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.formconfig.Option
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Field
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Value
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.exception.InvalidMappingException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Collection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.MultiValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.formconfig.Field as FormConfigField
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Collection as DataMappingCollection

class AnswersProviderTest {
  private val testAssessment = AssessmentAggregate().apply {
    answers.putAll(
      mapOf(
        Field.CURRENT_ACCOMMODATION.lower to SingleValue(Value.SETTLED.name),
        Field.FINANCE_INCOME.lower to MultiValue(listOf(Value.FAMILY_OR_FRIENDS.name)),
        Field.EDUCATION_DIFFICULTIES.lower to MultiValue(listOf("")),
      ),
    )
    collections.addLast(
      Collection(
        uuid = UUID.randomUUID(),
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
        name = DataMappingCollection.OFFENCE_ANALYSIS_VICTIM.name,
        items = mutableListOf(),
      ),
    )
  }

  private lateinit var sut: AnswersProvider

  @BeforeTest
  fun setUp() {
    val testFormConfig = FormConfig(
      "v1.0",
      mapOf(
        Field.CURRENT_ACCOMMODATION.lower to FormConfigField(
          Field.CURRENT_ACCOMMODATION.lower,
          listOf(Option(Value.TEMPORARY.name), Option(Value.NO_ACCOMMODATION.name), Option(Value.SETTLED.name)),
        ),
        Field.FINANCE_INCOME.lower to FormConfigField(
          Field.FINANCE_INCOME.lower,
          listOf(Option(Value.FAMILY_OR_FRIENDS.name)),
          FieldType.CHECKBOX,
        ),
        Field.EDUCATION_DIFFICULTIES.lower to FormConfigField(
          Field.EDUCATION_DIFFICULTIES.lower,
          listOf(Option("Some value")),
          FieldType.CHECKBOX,
        ),
        Field.OFFENCE_ANALYSIS_VICTIM_RELATIONSHIP.lower to FormConfigField(
          Field.OFFENCE_ANALYSIS_VICTIM_RELATIONSHIP.lower,
          listOf(Option("test option")),
          FieldType.RADIO,
          collection = DataMappingCollection.OFFENCE_ANALYSIS_VICTIM.name,
        ),
      ),
    )

    sut = AnswersProvider(testAssessment, testFormConfig)
  }

  @Nested
  inner class Answer {
    @Test
    fun `throws exception when field not in config`() {
      val exception = assertFailsWith<InvalidMappingException>(
        block = {
          sut.answer(Field.TEST_FIELD)
        },
      )
      assertEquals("Field test_field does not exist in form config version v1.0", exception.message)
    }

    @Test
    fun `returns the value of an existing answer`() {
      val answer = sut.answer(Field.CURRENT_ACCOMMODATION)

      assertEquals(Value.SETTLED.name, answer.value)

      val valuesException = assertFailsWith<InvalidMappingException>(
        block = { answer.values },
      )
      assertEquals("Invalid use of '.values' on a ${SingleValueAnswer::class.simpleName}", valuesException.message)

      val collectionException = assertFailsWith<InvalidMappingException>(
        block = { answer.collection },
      )
      assertEquals("Invalid use of '.collection' on a ${SingleValueAnswer::class.simpleName}", collectionException.message)
    }

    @Test
    fun `returns the checkbox values of an existing answer`() {
      val answer = sut.answer(Field.FINANCE_INCOME)

      assertEquals(listOf(Value.FAMILY_OR_FRIENDS.name), answer.values)

      val valueException = assertFailsWith<InvalidMappingException>(
        block = { answer.value },
      )
      assertEquals("Invalid use of '.value' on a ${MultipleValuesAnswer::class.simpleName}", valueException.message)

      val collectionException = assertFailsWith<InvalidMappingException>(
        block = { answer.collection },
      )
      assertEquals("Invalid use of '.collection' on a ${MultipleValuesAnswer::class.simpleName}", collectionException.message)
    }

    @Test
    fun `returns empty checkbox values when the answer is an array of empty string`() {
      val answer = sut.answer(Field.EDUCATION_DIFFICULTIES)
      assertEquals(emptyList(), answer.values)
    }

    @Test
    fun `returns the collection values of an existing answer`() {
      val answer = sut.answer(DataMappingCollection.OFFENCE_ANALYSIS_VICTIM)

      assertEquals(emptyList(), answer.collection)

      val valueException = assertFailsWith<InvalidMappingException>(
        block = { answer.value },
      )
      assertEquals("Invalid use of '.value' on a ${CollectionAnswer::class.simpleName}", valueException.message)

      val valuesException = assertFailsWith<InvalidMappingException>(
        block = { answer.values },
      )
      assertEquals("Invalid use of '.values' on a ${CollectionAnswer::class.simpleName}", valuesException.message)
    }
  }

  @Nested
  inner class Get {
    @Test
    fun `throws exception when called outside of a field context`() {
      val exception = assertFailsWith<InvalidMappingException>(
        block = {
          sut.get(Value.YES)
        },
      )
      assertEquals("Cannot obtain values without a field context. Call answer() first", exception.message)
    }

    @Test
    fun `throws exception for invalid field option`() {
      sut.answer(Field.FINANCE_INCOME)
      val exception = assertFailsWith<InvalidMappingException>(
        block = {
          sut.get(Value.NO_ACCOMMODATION)
        },
      )
      assertEquals(
        "NO_ACCOMMODATION is not a valid option for field finance_income in form config version v1.0",
        exception.message,
      )
    }

    @Test
    fun `returns value name for a valid field value`() {
      sut.answer(Field.FINANCE_INCOME)
      assertEquals("FAMILY_OR_FRIENDS", sut.get(Value.FAMILY_OR_FRIENDS))
    }
  }
}
