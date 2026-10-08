package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.Aggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AggregateState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.State
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.StateCollection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.UserDetailsEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.TimelineService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.UserDetailsService
import java.time.LocalDateTime
import java.util.UUID
import kotlin.reflect.KClass

class PersistenceContext(
  val stateService: StateService,
  val eventService: EventService,
  val timelineService: TimelineService,
  val userDetailsService: UserDetailsService,
  val assessmentService: AssessmentService,
) {
  val state: StateCollection = mutableMapOf()
  val events: MutableList<EventEntity<*>> = mutableListOf()
  val timeline: MutableList<TimelineEntity> = mutableListOf()
  val userDetails: MutableList<UserDetailsEntity> = mutableListOf()
  val assessments: MutableList<AssessmentEntity> = mutableListOf()

  fun persist() {
    userDetailsService.saveAll(userDetails).also { userDetails.clear() }
    assessmentService.saveAll(assessments).also { assessments.clear() }
    eventService.saveAll(events).also { events.clear() }
    stateService.persist(state).also { state.clear() }
    timelineService.saveAll(timeline).also { timeline.clear() }
  }

  fun findAssessment(uuid: UUID) = assessments.firstOrNull { it.uuid == uuid } ?: assessmentService.findBy(uuid).also { assessments.add(it) }
  fun findUserDetails(user: UserDetails) = userDetails.firstOrNull { it.userId == user.id && it.authSource == user.authSource } ?: userDetailsService.findOrCreate(user).also { userDetails.add(it) }

  private fun assessmentState(assessmentUuid: UUID): State = state.getOrPut(assessmentUuid) { mutableMapOf() }

  fun getAggregateState(assessment: AssessmentEntity, aggregateType: KClass<out Aggregate<*>>, pointInTime: LocalDateTime?): AggregateState<*> = assessmentState(assessment.uuid).getOrPut(aggregateType) {
    stateService.stateForType(aggregateType).fetchOrCreateState(assessment, pointInTime)
  }

  fun setAggregateState(assessmentUuid: UUID, aggregateType: KClass<out Aggregate<*>>, aggregateState: AggregateState<*>) {
    assessmentState(assessmentUuid)[aggregateType] = aggregateState
  }
}
