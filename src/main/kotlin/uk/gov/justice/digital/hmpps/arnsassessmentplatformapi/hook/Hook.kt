package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.hook

import com.fasterxml.jackson.annotation.JsonTypeInfo

// Plain interface, not sealed: implementations can live in any package and are discovered via classpath scan
// (see HookJacksonConfiguration) instead of a hand-maintained @JsonSubTypes list.
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
interface Hook

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class HookType(val name: String)
