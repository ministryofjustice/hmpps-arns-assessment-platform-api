package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.source.DataSourceContextHolder
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.source.ReplicationRoutingDataSource
import javax.sql.DataSource

@ConfigurationProperties(prefix = "spring.datasource.write")
class WriteProperties : DataSourceProperties()

@ConfigurationProperties(prefix = "spring.datasource.write-hikari")
class WriteHikariProperties : HikariConfig()

@ConfigurationProperties(prefix = "spring.datasource.read")
class ReadProperties : DataSourceProperties()

@ConfigurationProperties(prefix = "spring.datasource.read-hikari")
class ReadHikariProperties : HikariConfig()

@Configuration
@EnableConfigurationProperties(
  WriteProperties::class,
  WriteHikariProperties::class,
  ReadProperties::class,
  ReadHikariProperties::class,
)
class DataSourceConfig {

  @Bean("writeDataSource")
  @Primary
  fun writeDataSource(props: WriteProperties, hikariProps: WriteHikariProperties) = dataSource(props, hikariProps)

  @Bean("readDataSource")
  fun readDataSource(props: ReadProperties, hikariProps: ReadHikariProperties) = dataSource(props, hikariProps)

  @Bean
  fun routingDataSource(
    @Qualifier("writeDataSource") writeDataSource: DataSource,
    @Qualifier("readDataSource") readDataSource: DataSource,
  ): DataSource = ReplicationRoutingDataSource().apply {
    setTargetDataSources(
      mapOf(
        DataSourceContextHolder.WRITE to writeDataSource,
        DataSourceContextHolder.READ to readDataSource,
      ),
    )
    setDefaultTargetDataSource(writeDataSource)
    afterPropertiesSet()
  }

  companion object {
    fun dataSource(props: DataSourceProperties, hikariProps: HikariConfig) = HikariDataSource(
      hikariProps.apply {
        jdbcUrl = props.url
        username = props.username
        password = props.password
        driverClassName = props.driverClassName
      },
    )
  }
}
