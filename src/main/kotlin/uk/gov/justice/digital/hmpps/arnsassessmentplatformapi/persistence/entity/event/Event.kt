package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event

import com.fasterxml.jackson.annotation.JsonTypeInfo
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance

interface EventType {
  val eventType: String
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed interface Event

val <T : Event> KClass<T>.eventType: String
  get() = (this.companionObjectInstance as? EventType)
    ?.eventType ?: error("No EventCompanion on ${this.qualifiedName}")

val Event.eventType: String
  get() = this::class.eventType
