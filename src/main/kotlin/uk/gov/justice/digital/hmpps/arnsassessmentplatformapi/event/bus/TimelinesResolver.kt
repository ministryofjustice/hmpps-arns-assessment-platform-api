package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline

interface TimelinesResolver {
  fun with(customTimeline: Timeline?)
}
