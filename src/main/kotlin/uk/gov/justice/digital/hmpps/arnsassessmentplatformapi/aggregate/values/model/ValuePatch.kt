package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.values.model

import com.github.difflib.patch.Patch
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Value
import java.time.LocalDateTime
import kotlin.reflect.KClass

data class ValuePatch(
  val patch: Patch<String>,
  val type: KClass<out Value>,
  val dateCreated: LocalDateTime = LocalDateTime.now(),
)
