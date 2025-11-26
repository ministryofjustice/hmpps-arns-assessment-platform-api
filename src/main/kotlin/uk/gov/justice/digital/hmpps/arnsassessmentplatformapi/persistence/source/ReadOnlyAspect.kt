package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.source

import jakarta.transaction.Transactional
import org.aspectj.lang.annotation.After
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Aspect
@Component
class ReadOnlyAspect: Ordered {

  override fun getOrder(): Int {
    val base = Transactional::class.java.getAnnotation(Order::class.java)?.value ?: 0
    return base - 1 // lower number results in higher precedence. ReadOnly MUST apply before Transactional
  }

  @Before("@annotation(ReadOnly)")
  fun setReadOnly() {
    DataSourceContextHolder.set(DataSourceContextHolder.READ)
  }

  @After("@annotation(ReadOnly)")
  fun clear() {
    DataSourceContextHolder.clear()
  }
}
