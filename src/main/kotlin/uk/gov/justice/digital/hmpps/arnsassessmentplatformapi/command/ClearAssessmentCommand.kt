package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.Reference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.hook.Hook

/**
 * Clears an assessment so it can start again, e.g. after a change of pathway.
 *
 * All answers are always removed. Only the properties and collections named here are cleared;
 * anything not named (e.g. flags, other properties) is kept.
 *
 * @property properties property keys to remove. `*` matches any characters, e.g. `*_section_complete`.
 *   Keys that don't exist are ignored.
 * @property collections names of top-level collections to empty. The collections are kept, but their
 *   items are removed. Names that don't exist are ignored.
 */
data class ClearAssessmentCommand(
  override val user: UserDetails,
  override val assessmentUuid: Reference,
  val properties: List<String> = emptyList(),
  val collections: List<String> = emptyList(),
  override val timeline: Timeline? = null,
  override val hooks: List<Hook>? = null,
  override val autosaved: Boolean = false,
) : RequestableCommand
