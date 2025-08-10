package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event

import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed interface Event
