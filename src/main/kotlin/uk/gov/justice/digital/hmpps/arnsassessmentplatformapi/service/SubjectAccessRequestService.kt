package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierPair
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierType
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.SubjectAccessRequestQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.bus.QueryBus
import uk.gov.justice.hmpps.kotlin.sar.HmppsPrisonProbationSubjectAccessRequestService
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class SubjectAccessRequestService(
  private val queryBus: QueryBus,
) : HmppsPrisonProbationSubjectAccessRequestService {
  override fun getContentFor(
    prn: String?,
    crn: String?,
    fromDate: LocalDate?,
    toDate: LocalDate?,
  ): HmppsSubjectAccessRequestContent? {
    val content = queryBus.dispatch(
      SubjectAccessRequestQuery(
        timestamp = LocalDateTime.now(),
        assessmentIdentifiers = buildSet {
          crn?.let { add(IdentifierPair(IdentifierType.CRN, it)) }
          prn?.let { add(IdentifierPair(IdentifierType.PRN, it)) }
        },
      ),
    )

    return HmppsSubjectAccessRequestContent(
      content = content,
    )
  }
}
