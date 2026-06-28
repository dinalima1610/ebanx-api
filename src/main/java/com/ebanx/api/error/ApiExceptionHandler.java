package com.ebanx.api.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduz falhas previstas de negócio para o contrato exigido pelo Ipkiss Tester.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Integer> handleBusinessException(BusinessException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(0);
    }
}
