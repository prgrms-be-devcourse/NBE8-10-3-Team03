package com.back.global.globalExceptionHandler

import com.back.global.exception.ServiceException
import com.back.global.rsData.RsData
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.util.NoSuchElementException

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(NoSuchElementException::class)
    fun handle(ex: NoSuchElementException): ResponseEntity<RsData<Void>> {
        return ResponseEntity(
            RsData(
                "404-1",
                "해당 데이터가 존재하지 않습니다."
            ),
            NOT_FOUND
        )
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handle(ex: ConstraintViolationException): ResponseEntity<RsData<Void>> {
        val message = ex.constraintViolations
            .map { violation ->
                val field = violation.propertyPath.toString().split(".", limit = 2)[1]
                val messageTemplateBits = violation.messageTemplate.split(".")
                val code = messageTemplateBits[messageTemplateBits.size - 2]
                val errorMessage = violation.message

                "$field-$code-$errorMessage"
            }
            .sorted()
            .joinToString("\n")

        return ResponseEntity(
            RsData(
                "400-1",
                message
            ),
            BAD_REQUEST
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handle(ex: MethodArgumentNotValidException): ResponseEntity<RsData<Void>> {
        val message = ex.bindingResult
            .allErrors
            .filterIsInstance<FieldError>()
            .mapNotNull { it.defaultMessage }
            .sorted()
            .joinToString(" ")

        return ResponseEntity(
            RsData(
                "400-1",
                message
            ),
            BAD_REQUEST
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handle(ex: HttpMessageNotReadableException): ResponseEntity<RsData<Void>> {
        return ResponseEntity(
            RsData(
                "400-1",
                "요청 본문이 올바르지 않습니다."
            ),
            BAD_REQUEST
        )
    }

    @ExceptionHandler(MissingRequestHeaderException::class)
    fun handle(ex: MissingRequestHeaderException): ResponseEntity<RsData<Void>> {
        return ResponseEntity(
            RsData(
                "400-1",
                "${ex.headerName}-NotBlank-${ex.localizedMessage}"
            ),
            BAD_REQUEST
        )
    }

    @ExceptionHandler(ServiceException::class)
    fun handle(ex: ServiceException, response: HttpServletResponse): RsData<Void> {
        val rsData = ex.rsData
        response.status = rsData.statusCode
        return rsData
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handle(ex: IllegalArgumentException): ResponseEntity<RsData<Void>> {
        return ResponseEntity(
            RsData(
                "400-1",
                ex.message
            ),
            BAD_REQUEST
        )
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handle(ex: IllegalStateException): ResponseEntity<RsData<Void>> {
        return ResponseEntity(
            RsData(
                "400-2",
                ex.message
            ),
            BAD_REQUEST
        )
    }
}
