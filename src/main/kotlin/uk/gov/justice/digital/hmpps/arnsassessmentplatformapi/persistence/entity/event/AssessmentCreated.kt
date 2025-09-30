package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event

import com.fasterxml.jackson.annotation.JsonTypeName

@JsonTypeName("ASSESSMENT_CREATED")
class AssessmentCreated : Event
