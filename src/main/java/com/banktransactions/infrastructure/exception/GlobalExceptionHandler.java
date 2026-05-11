package com.banktransactions.infrastructure.exception;

import com.banktransactions.features.importing.domain.exception.ImportJobNotFoundException;
import com.banktransactions.features.importing.domain.exception.InvalidCsvStructureException;
import com.banktransactions.features.importing.domain.exception.TooManyRowsException;
import com.banktransactions.features.importing.domain.exception.UnsupportedFileTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Centralny handler błędów REST.
 * Zwraca ProblemDetail (RFC 7807) przez ResponseEntity.
 * Kody błędów:
 *  404  import-job-not-found, endpoint-not-found
 *  400  bad-request
 *  415  unsupported-file-type
 *  500  internal-error
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── 404 ──────────────────────────────────────────────────────────────────

    @ExceptionHandler(ImportJobNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(ImportJobNotFoundException ex) {
        log.warn("Import job not found: {}", ex.getMessage());
        return respond(HttpStatus.NOT_FOUND, "import-job-not-found", ex.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResource(NoResourceFoundException ex) {
        return respond(HttpStatus.NOT_FOUND, "endpoint-not-found", ex.getMessage());
    }

    // ── 400 ──────────────────────────────────────────────────────────────────

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleBadRequest(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return respond(HttpStatus.BAD_REQUEST, "bad-request", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String detail;
        Class<?> requiredType = ex.getRequiredType();
        if (requiredType != null && requiredType.isEnum()) {
            String allowed = Arrays.stream(requiredType.getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            detail = "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName()
                    + "'. Allowed values: " + allowed;
        } else {
            detail = "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'.";
        }
        log.warn("Type mismatch: {}", detail);
        return respond(HttpStatus.BAD_REQUEST, "bad-request", detail);
    }


    // ── 413 ──────────────────────────────────────────────────────────────────

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> handleFileTooLarge(MaxUploadSizeExceededException ignored) {
        return respond(HttpStatus.CONTENT_TOO_LARGE, "file-too-large",
                "Uploaded file exceeds the maximum allowed size of 50 MB.");
    }

    // ── 415 ──────────────────────────────────────────────────────────────────

    @ExceptionHandler(UnsupportedFileTypeException.class)
    public ResponseEntity<ProblemDetail> handleUnsupportedFileType(UnsupportedFileTypeException ex) {
        return respond(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "unsupported-file-type", ex.getMessage());
    }

    // ── 422 ──────────────────────────────────────────────────────────────────

    @ExceptionHandler(InvalidCsvStructureException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCsvStructure(InvalidCsvStructureException ex) {
        log.warn("Invalid CSV structure: {}", ex.getMessage());
        return respond(HttpStatus.valueOf(422), "invalid-csv-structure", ex.getMessage());
    }

    @ExceptionHandler(TooManyRowsException.class)
    public ResponseEntity<ProblemDetail> handleTooManyRows(TooManyRowsException ex) {
        log.warn("CSV row limit exceeded: actual={}, limit={}", ex.getActual(), ex.getLimit());
        return respond(HttpStatus.valueOf(422), "too-many-rows", ex.getMessage());
    }

    // ── 500 ──────────────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneral(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error",
                "An unexpected error occurred. Please try again later.");
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private ResponseEntity<ProblemDetail> respond(HttpStatus status, String errorCode, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create("https://banktransactions.com/errors/" + errorCode));
        pd.setProperty("errorCode", errorCode);
        return ResponseEntity.status(status).body(pd);
    }
}
