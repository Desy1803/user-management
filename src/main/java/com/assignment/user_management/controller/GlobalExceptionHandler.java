package com.assignment.user_management.controller;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import com.assignment.user_management.exception.CsvImportException;
import com.assignment.user_management.exception.EmailAlreadyExistsException;
import com.assignment.user_management.exception.ResourceNotFoundException;
import com.assignment.user_management.model.ErrorResponse;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({ResourceNotFoundException.class, EntityNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleResourceNotFound(RuntimeException ex) {
        logger.warn("Risorsa non trovata: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Resource Not Found",
                ex.getMessage()
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(CsvImportException.class)
    public ResponseEntity<ErrorResponse> handleCsvImportError(CsvImportException ex) {
        logger.error("Errore durante l'importazione del CSV: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "CSV Import Error",
                ex.getMessage()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        logger.warn("Tentativo di inserimento email duplicata: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(), 
                "Conflict",
                ex.getMessage()
        );
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ErrorResponse> handleBindingAndValidationErrors(Exception ex) {
        Map<String, String> errors = new HashMap<>();
        
        if (ex instanceof MethodArgumentNotValidException validationEx) {
            errors = extractFieldErrors(validationEx.getBindingResult());
        } else if (ex instanceof BindException bindEx) {
            errors = extractFieldErrors(bindEx.getBindingResult());
        }

        logger.warn("Errore di validazione/binding dei dati: {}", errors);
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                "I dati o i parametri inviati non sono validi",
                errors
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath().toString(), 
                        v -> v.getMessage(), 
                        (existing, replacement) -> existing
                ));

        logger.warn("Violazione dei vincoli di validazione: {}", errors);
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                "I vincoli di validazione della richiesta non sono stati rispettati",
                errors
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class, 
        MissingServletRequestPartException.class, 
        MultipartException.class, 
        MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequestInfrastructure(Exception ex) {
        String detailMessage = "La richiesta è malformata o mancano dei parametri fondamentali.";
        
        if (ex instanceof HttpMessageNotReadableException) {
            detailMessage = "Corpo della richiesta non leggibile o JSON sintatticamente non valido.";
        } else if (ex instanceof MissingServletRequestPartException missingPartEx) {
            detailMessage = String.format("Parte della richiesta multipart mancante: %s", missingPartEx.getRequestPartName());
        } else if (ex instanceof MultipartException) {
            detailMessage = "Errore nel caricamento del file multipart. Verificare il formato o la dimensione.";
        } else if (ex instanceof MethodArgumentTypeMismatchException mismatchEx) {
            detailMessage = String.format("Il parametro '%s' ha un valore o un tipo errato.", mismatchEx.getName());
        }

        logger.warn("Bad Request infrastrutturale: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                detailMessage
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        logger.error("Errore interno imprevisto: ", ex);
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Si è verificato un errore interno nel server."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private Map<String, String> extractFieldErrors(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField, 
                        FieldError::getDefaultMessage, 
                        (existing, replacement) -> existing
                ));
    }
}