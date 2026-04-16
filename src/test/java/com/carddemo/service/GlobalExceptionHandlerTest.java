package com.carddemo.service;

import com.carddemo.exception.ApplicationException;
import com.carddemo.exception.GlobalExceptionHandler;
import com.carddemo.service.MessageService.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 *
 * <p>Verifies that:
 * <ul>
 *   <li>{@link ApplicationException} is mapped to the correct HTTP status based
 *       on the {@link ErrorCode} severity.</li>
 *   <li>The error response body contains all required fields
 *       (timestamp, status, errorCode, severity, message, path).</li>
 *   <li>General {@link Exception} falls back to 500.</li>
 *   <li>{@link IllegalArgumentException} maps to 400.</li>
 * </ul>
 */
@DisplayName("GlobalExceptionHandler – REST exception mapping tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private WebRequest              request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        MockHttpServletRequest mockReq = new MockHttpServletRequest("GET", "/api/test");
        request = new ServletWebRequest(mockReq);
    }

    // =========================================================================
    // 1. ApplicationException → HTTP status mapping by severity
    // =========================================================================

    @Nested
    @DisplayName("ApplicationException HTTP status mapping (severity → HTTP code)")
    class ApplicationExceptionMappingTests {

        @Test
        @DisplayName("Severity 3 (date validation) → 400 Bad Request")
        void dateValidationError_returns400() {
            ApplicationException ex = new ApplicationException(
                    ErrorCode.DATE_BAD_VALUE, "date=20231301");
            ResponseEntity<Map<String, Object>> response =
                    handler.handleApplicationException(ex, request);
            assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
        }

        @Test
        @DisplayName("Severity 4 (input validation) → 400 Bad Request")
        void inputValidationError_returns400() {
            ApplicationException ex = new ApplicationException(ErrorCode.INPUT_REQUIRED);
            ResponseEntity<Map<String, Object>> response =
                    handler.handleApplicationException(ex, request);
            assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
        }

        @Test
        @DisplayName("ACCT_NOT_FOUND (severity 8) → 404 Not Found")
        void acctNotFound_returns404() {
            ApplicationException ex = new ApplicationException(ErrorCode.ACCT_NOT_FOUND);
            ResponseEntity<Map<String, Object>> response =
                    handler.handleApplicationException(ex, request);
            assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCode().value());
        }

        @Test
        @DisplayName("CARD_NOT_FOUND (severity 8) → 404 Not Found")
        void cardNotFound_returns404() {
            ApplicationException ex = new ApplicationException(ErrorCode.CARD_NOT_FOUND);
            ResponseEntity<Map<String, Object>> response =
                    handler.handleApplicationException(ex, request);
            assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCode().value());
        }

        @Test
        @DisplayName("TXN_DUPLICATE (severity 8) → 422 Unprocessable Entity")
        void txnDuplicate_returns422() {
            ApplicationException ex = new ApplicationException(ErrorCode.TXN_DUPLICATE);
            ResponseEntity<Map<String, Object>> response =
                    handler.handleApplicationException(ex, request);
            assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(),
                    response.getStatusCode().value());
        }

        @Test
        @DisplayName("ABEND_GENERAL (severity 12) → 500 Internal Server Error")
        void abendGeneral_returns500() {
            ApplicationException ex = new ApplicationException(ErrorCode.ABEND_GENERAL);
            ResponseEntity<Map<String, Object>> response =
                    handler.handleApplicationException(ex, request);
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    response.getStatusCode().value());
        }

        @Test
        @DisplayName("ABEND_CICS (severity 12) → 500 Internal Server Error")
        void abendCics_returns500() {
            ApplicationException ex = new ApplicationException(ErrorCode.ABEND_CICS);
            ResponseEntity<Map<String, Object>> response =
                    handler.handleApplicationException(ex, request);
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    response.getStatusCode().value());
        }
    }

    // =========================================================================
    // 2. Response body structure
    // =========================================================================

    @Nested
    @DisplayName("Response body contains all required fields")
    class ResponseBodyStructureTests {

        @Test
        @DisplayName("Body contains: timestamp, status, errorCode, severity, message, path")
        void body_containsAllRequiredFields() {
            ApplicationException ex = new ApplicationException(
                    ErrorCode.DATE_BAD_VALUE, "20231301");
            ResponseEntity<Map<String, Object>> response =
                    handler.handleApplicationException(ex, request);

            Map<String, Object> body = response.getBody();
            assertNotNull(body);
            assertTrue(body.containsKey("timestamp"),  "missing 'timestamp'");
            assertTrue(body.containsKey("status"),     "missing 'status'");
            assertTrue(body.containsKey("errorCode"),  "missing 'errorCode'");
            assertTrue(body.containsKey("severity"),   "missing 'severity'");
            assertTrue(body.containsKey("message"),    "missing 'message'");
            assertTrue(body.containsKey("path"),       "missing 'path'");
        }

        @Test
        @DisplayName("'errorCode' in body matches ErrorCode.getMessageCode()")
        void body_errorCodeMatchesMessageCode() {
            ApplicationException ex = new ApplicationException(ErrorCode.DATE_INSUFFICIENT);
            ResponseEntity<Map<String, Object>> response =
                    handler.handleApplicationException(ex, request);
            Map<String, Object> body = response.getBody();
            assertNotNull(body);
            assertEquals("09CB", body.get("errorCode"));
        }

        @Test
        @DisplayName("'severity' in body matches ErrorCode.getSeverityCode()")
        void body_severityMatchesSeverityCode() {
            ApplicationException ex = new ApplicationException(ErrorCode.ABEND_GENERAL);
            ResponseEntity<Map<String, Object>> response =
                    handler.handleApplicationException(ex, request);
            Map<String, Object> body = response.getBody();
            assertNotNull(body);
            assertEquals(12, body.get("severity"));
        }

        @Test
        @DisplayName("'message' in body contains exception message")
        void body_messageContainsExceptionMessage() {
            ApplicationException ex = new ApplicationException(
                    ErrorCode.CARD_INACTIVE, "context-info");
            ResponseEntity<Map<String, Object>> response =
                    handler.handleApplicationException(ex, request);
            Map<String, Object> body = response.getBody();
            assertNotNull(body);
            String message = (String) body.get("message");
            assertNotNull(message);
            assertFalse(message.isEmpty());
        }

        @Test
        @DisplayName("'status' in body matches HTTP status code integer")
        void body_statusMatchesHttpStatusCode() {
            ApplicationException ex = new ApplicationException(ErrorCode.ACCT_NOT_FOUND);
            ResponseEntity<Map<String, Object>> response =
                    handler.handleApplicationException(ex, request);
            Map<String, Object> body = response.getBody();
            assertNotNull(body);
            assertEquals(404, body.get("status"));
        }
    }

    // =========================================================================
    // 3. IllegalArgumentException → 400
    // =========================================================================

    @Nested
    @DisplayName("IllegalArgumentException handling (COBOL INVALID KEY equivalent)")
    class IllegalArgumentTests {

        @Test
        @DisplayName("IllegalArgumentException → 400 Bad Request")
        void illegalArgument_returns400() {
            IllegalArgumentException ex = new IllegalArgumentException("bad arg");
            ResponseEntity<Map<String, Object>> response =
                    handler.handleIllegalArgument(ex, request);
            assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
        }

        @Test
        @DisplayName("IllegalArgumentException body contains message text")
        void illegalArgument_bodyContainsMessage() {
            IllegalArgumentException ex = new IllegalArgumentException("invalid format");
            ResponseEntity<Map<String, Object>> response =
                    handler.handleIllegalArgument(ex, request);
            Map<String, Object> body = response.getBody();
            assertNotNull(body);
            assertTrue(body.containsKey("message"));
        }
    }

    // =========================================================================
    // 4. General Exception → 500 (COBOL WHEN OTHER / 9000-ABEND)
    // =========================================================================

    @Nested
    @DisplayName("General Exception handling (COBOL WHEN OTHER catch-all)")
    class GeneralExceptionTests {

        @Test
        @DisplayName("Unexpected Exception → 500 Internal Server Error")
        void generalException_returns500() {
            Exception ex = new RuntimeException("unexpected");
            ResponseEntity<Map<String, Object>> response =
                    handler.handleGeneral(ex, request);
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    response.getStatusCode().value());
        }

        @Test
        @DisplayName("500 body contains ABEND_GENERAL error code")
        void generalException_bodyContainsAbendCode() {
            Exception ex = new RuntimeException("boom");
            ResponseEntity<Map<String, Object>> response =
                    handler.handleGeneral(ex, request);
            Map<String, Object> body = response.getBody();
            assertNotNull(body);
            assertEquals("A999", body.get("errorCode"));
            assertEquals(12, body.get("severity"));
        }
    }
}
