package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

class GroupEvent : Event {
  // .. because we can't have data classes with no args
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    return true
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
