package com.faforever.userservice.config

import io.micronaut.core.convert.ConversionContext
import io.micronaut.core.convert.TypeConverter
import jakarta.inject.Singleton
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.util.*

@Singleton
class OffsetDateTimeConverter : TypeConverter<String?, OffsetDateTime> {
    override fun convert(
        inputDateTime: String?,
        targetType: Class<OffsetDateTime>,
        context: ConversionContext
    ): Optional<OffsetDateTime> {
        if (inputDateTime != null) {
            try {
                val offsetDateTime = OffsetDateTime.parse(inputDateTime)
                return Optional.of(offsetDateTime)
            } catch (e: DateTimeParseException) {
                context.reject(inputDateTime, e)
            }
        }
        return Optional.empty()
    }
}
