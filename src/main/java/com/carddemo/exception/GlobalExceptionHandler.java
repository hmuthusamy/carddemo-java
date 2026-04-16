package com.carddemo.exception;

import com.carddemo.service.MessageService.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * GlobalExceptionHandler – Spring REST controller advice that catches
 * {@link ApplicationException} (and common Spring exceptions) and translates
 * them into structured JSON error responses.
 *
 * <h3>COBOL equivalent</h3>
 * <p>In the COBOL programs every error path called a common error paragraph
 * (e.g. {@code 9000-ERROR} in CODATE01.cbl) that populated
 * {@code MQ-ERR-DISPLAY} / {@code WS-MESSAGE} and set RETURN-CODE before
 * GOBACK.  This class is the Java equivalent: it intercepts exceptions thrown
 * by service-layer code and returns a well-formed HTTP error response instead.
 *
 * <h3>Error response format</h3>
 * <pre>
 * {
 *   "timestamp":    "2024-01-01T10:00:00",
 *   "status":       400,
 *   "errorCode":    "V001",
 *   "severity":     4,
 *   "message":      "Required field is missing [accountId]",
 *   "path":         "/api/accounts"
 * }
 * </pre>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // -----------------------------------------------------------------------
    // ApplicationException → HTTP mapping
    // -----------------------------------------------------------------------

    /**
     * Maps {@link ApplicationException} to an HTTP response based on the
     * severity encoded in its {@link ErrorCode}.
     *
     * <pre>
     *   severity 0       → 200 OK      (should not normally be thrown)
     *   severity 3-4     → 400 Bad Request  (date/input validation)
     *   severity 8       → 404 / 422        (business logic / not found)
     *   severity 12+     → 500 Internal     (hard abend)
     * </pre>
     */
    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<Map<String, Object>> handleApplicationException(
            ApplicationException ex, WebRequest request) {

        ErrorCode ec     = ex.getErrorCode();
        HttpStatus status = resolveHttpStatus(ec);

        if (ec.isAbend()) {
            log.error("ABEND [{}] severity={} context={}",
                    ec.getMessageCode(), ec.getSeverityCode(), ex.getContext(), ex);
        } else {
            log.warn("ApplicationException [{}] severity={} context={}",
                    ec.getMessageCode(), ec.getSeverityCode(), ex.getContext());
        }

        return ResponseEntity.status(status)
                .body(buildBody(status, ec.getMessageCode(), ec.getSeverityCode(),
                        ex.getMessage(), request));
    }

    // -----------------------------------------------------------------------
    // Bean validation failures (@Valid / @Validated)
    // -----------------------------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex, WebRequest request) {

        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.toList());

        log.warn("Validation failed: {}", errors);

        Map<String, Object> body = buildBody(
                HttpStatus.BAD_REQUEST,
                ErrorCode.INPUT_REQUIRED.getMessageCode(),
                ErrorCode.INPUT_REQUIRED.getSeverityCode(),
                "Validation failed",
                request);
        body.put("validationErrors", errors);
        return ResponseEntity.badRequest().body(body);
    }

    // -----------------------------------------------------------------------
    // Illegal argument (mirrors COBOL INVALID KEY / AT END branches)
    // -----------------------------------------------------------------------

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest request) {

        log.warn("IllegalArgumentException: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(buildBody(HttpStatus.BAD_REQUEST,
                        ErrorCode.INPUT_INVALID_CHARS.getMessageCode(),
                        ErrorCode.INPUT_INVALID_CHARS.getSeverityCode(),
                        ex.getMessage(), request));
    }

    // -----------------------------------------------------------------------
    // Catch-all (COBOL: WHEN OTHER in EVALUATE / 9000-ABEND)
    // -----------------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(
            Exception ex, WebRequest request) {

        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildBody(HttpStatus.INTERNAL_SERVER_ERROR,
                        ErrorCode.ABEND_GENERAL.getMessageCode(),
                        ErrorCode.ABEND_GENERAL.getSeverityCode(),
                        "An unexpected error occurred", request));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private HttpStatus resolveHttpStatus(ErrorCode ec) {
        if (ec.getSeverityCode() >= 12)         return HttpStatus.INTERNAL_SERVER_ERROR;
        if (ec == ErrorCode.ACCT_NOT_FOUND
         || ec == ErrorCode.CARD_NOT_FOUND
         || ec == ErrorCode.CUST_NOT_FOUND
         || ec == ErrorCode.USER_NOT_FOUND)     return HttpStatus.NOT_FOUND;
        if (ec.getSeverityCode() >= 8)          return HttpStatus.UNPROCESSABLE_ENTITY;
        if (ec.getSeverityCode() >= 3)          return HttpStatus.BAD_REQUEST;
        return HttpStatus.OK;
    }

    private Map<String, Object> buildBody(HttpStatus status, String errorCode,
                                          int severity, String message,
                                          WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp",  LocalDateTime.now().toString());
        body.put("status",     status.value());
        body.put("errorCode",  errorCode);
        body.put("severity",   severity);
        body.put("message",    message);
        body.put("path",       request.getDescription(false).replace("uri=", ""));
        return body;
    }
}
