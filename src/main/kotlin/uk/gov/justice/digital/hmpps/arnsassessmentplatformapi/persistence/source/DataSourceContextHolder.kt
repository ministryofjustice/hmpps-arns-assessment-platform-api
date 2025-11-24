package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.source

object DataSourceContextHolder {
  private val CONTEXT = ThreadLocal<String>()

  const val WRITE = "write"
  const val READ = "read"

  fun set(mode: String) = CONTEXT.set(mode)
  fun get(): String = CONTEXT.get() ?: WRITE
  fun clear() = CONTEXT.remove()
}
