package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.source

import org.aspectj.lang.annotation.After
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.springframework.stereotype.Component

@Aspect
@Component
class ReadOnlyAspect {

  @Before("@annotation(ReadOnly)")
  fun setReadOnly() {
    DataSourceContextHolder.set(DataSourceContextHolder.READ)
  }

  @After("@annotation(ReadOnly)")
  fun clear() {
    DataSourceContextHolder.clear()
  }
}
