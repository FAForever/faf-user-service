package com.faforever.userservice.error

import org.slf4j.Logger
import org.springframework.beans.ConversionNotSupportedException
import org.springframework.beans.TypeMismatchException
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.http.converter.HttpMessageNotWritableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.validation.BindException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ServerWebInputException

@RestControllerAdvice
class GlobalExceptionHandler {
    companion object {
        private val LOG: Logger = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }

    @ExceptionHandler(
        ServerWebInputException::class,
        ConversionNotSupportedException::class,
        TypeMismatchException::class,
        HttpMessageNotReadableException::class,
        HttpMessageNotWritableException::class,
        MethodArgumentNotValidException::class,
        BindException::class
    )
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    fun processBadRequests(ex: Exception): ErrorResponse {
        LOG.debug("Bad request", ex)
        return ErrorResponse(listOf(StringOnlyErrorMessage(ex.message!!)))
    }

    @ExceptionHandler(AccessDeniedException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ResponseBody
    fun processAccessDenied(ex: Exception): ErrorResponse {
        LOG.debug("Forbidden", ex)
        return ErrorResponse(listOf(StringOnlyErrorMessage(ex.message!!)))
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handle(e: Exception) {
        LOG.error("Unknown internal error occurred.", e)
    }
}
