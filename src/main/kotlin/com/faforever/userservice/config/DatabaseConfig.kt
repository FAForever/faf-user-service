package com.faforever.userservice.config

import io.r2dbc.spi.ConnectionFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.CustomConversions
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions
import org.springframework.data.r2dbc.dialect.DialectResolver
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

@ReadingConverter
class LocalDateTimeToOffsetDateTimeReadingConverter : Converter<LocalDateTime, OffsetDateTime> {
    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(LocalDateTimeToOffsetDateTimeReadingConverter::class.java)
    }

    override fun convert(source: LocalDateTime): OffsetDateTime {
        LOG.trace("convert() called with: source = $source")
        return source.atOffset(ZoneOffset.UTC)
    }
}

@Configuration
class DatabaseConfiguration {
    @Bean
    fun r2dbcCustomConversions(connectionFactory: ConnectionFactory): R2dbcCustomConversions {
        val dialect = DialectResolver.getDialect(connectionFactory)
        val converters = dialect.converters + R2dbcCustomConversions.STORE_CONVERTERS
        val storeConversions = CustomConversions.StoreConversions.of(dialect.simpleTypeHolder, converters)
        val converterList = listOf<Converter<*, *>>(LocalDateTimeToOffsetDateTimeReadingConverter())
        return R2dbcCustomConversions(storeConversions, converterList)
    }
}
