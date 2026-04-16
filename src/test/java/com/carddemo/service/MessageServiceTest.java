package com.carddemo.service;

import com.carddemo.exception.ApplicationException;
import com.carddemo.service.MessageService.AbendData;
import com.carddemo.service.MessageService.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MessageService} and its inner {@link ErrorCode} enum.
 *
 * <p>COBOL sources covered:
 * <ul>
 *   <li>CSMSG01Y.cpy – CCDA-COMMON-MESSAGES literals</li>
 *   <li>CSMSG02Y.cpy – ABEND-DATA structure</li>
 *   <li>CSUTLDTC.cbl – CEEDAYS feedback message texts</li>
 * </ul>
 */
@DisplayName("MessageService – COBOL CSMSG01Y / CSMSG02Y / CSUTLDTC migration tests")
class MessageServiceTest {

    private MessageService service;

    @BeforeEach
    void setUp() {
        service = new MessageService();
    }

    // =========================================================================
    // 1. ErrorCode enum – severity / message code properties
    // =========================================================================

    @Nested
    @DisplayName("ErrorCode enum properties")
    class ErrorCodeTests {

        @Test
        @DisplayName("SUCCESS has severity 0 and isOk() == true")
        void success_isOk() {
            assertTrue(ErrorCode.SUCCESS.isOk());
            assertEquals(0, ErrorCode.SUCCESS.getSeverityCode());
        }

        @Test
        @DisplayName("DATE_VALID has severity 0 (FC-INVALID-DATE = 'Date is valid')")
        void dateValid_severity0() {
            assertEquals(0, ErrorCode.DATE_VALID.getSeverityCode());
            assertTrue(ErrorCode.DATE_VALID.getDescription().contains("valid"));
        }

        @Test
        @DisplayName("DATE_INSUFFICIENT maps FC-INSUFFICIENT-DATA (severity 3)")
        void dateInsufficient_severity3() {
            assertEquals(3, ErrorCode.DATE_INSUFFICIENT.getSeverityCode());
            assertEquals("09CB", ErrorCode.DATE_INSUFFICIENT.getMessageCode());
        }

        @Test
        @DisplayName("DATE_BAD_VALUE maps FC-BAD-DATE-VALUE (severity 3)")
        void dateBadValue_severity3() {
            assertEquals(3, ErrorCode.DATE_BAD_VALUE.getSeverityCode());
            assertEquals("09CC", ErrorCode.DATE_BAD_VALUE.getMessageCode());
        }

        @Test
        @DisplayName("DATE_INVALID_ERA maps FC-INVALID-ERA (severity 3)")
        void dateInvalidEra_severity3() {
            assertEquals(3, ErrorCode.DATE_INVALID_ERA.getSeverityCode());
            assertEquals("09CD", ErrorCode.DATE_INVALID_ERA.getMessageCode());
        }

        @Test
        @DisplayName("DATE_INVALID_MONTH maps FC-INVALID-MONTH (severity 3)")
        void dateInvalidMonth_severity3() {
            assertEquals(3, ErrorCode.DATE_INVALID_MONTH.getSeverityCode());
            assertEquals("09D5", ErrorCode.DATE_INVALID_MONTH.getMessageCode());
        }

        @Test
        @DisplayName("DATE_BAD_PIC_STRING maps FC-BAD-PIC-STRING (severity 3)")
        void dateBadPicString_severity3() {
            assertEquals(3, ErrorCode.DATE_BAD_PIC_STRING.getSeverityCode());
            assertEquals("09D6", ErrorCode.DATE_BAD_PIC_STRING.getMessageCode());
        }

        @Test
        @DisplayName("DATE_NON_NUMERIC maps FC-NON-NUMERIC-DATA (severity 3)")
        void dateNonNumeric_severity3() {
            assertEquals(3, ErrorCode.DATE_NON_NUMERIC.getSeverityCode());
            assertEquals("09D8", ErrorCode.DATE_NON_NUMERIC.getMessageCode());
        }

        @Test
        @DisplayName("DATE_ERA_YEAR_ZERO maps FC-YEAR-IN-ERA-ZERO (severity 3)")
        void dateEraYearZero_severity3() {
            assertEquals(3, ErrorCode.DATE_ERA_YEAR_ZERO.getSeverityCode());
            assertEquals("09D9", ErrorCode.DATE_ERA_YEAR_ZERO.getMessageCode());
        }

        @Test
        @DisplayName("Abend codes have severity 12 and isAbend() == true")
        void abendCodes_severity12() {
            assertTrue(ErrorCode.ABEND_GENERAL.isAbend());
            assertEquals(12, ErrorCode.ABEND_GENERAL.getSeverityCode());
            assertTrue(ErrorCode.ABEND_CICS.isAbend());
            assertTrue(ErrorCode.ABEND_FILE_IO.isAbend());
        }

        @Test
        @DisplayName("Business errors have severity 8 and isError() == true")
        void businessErrors_severity8() {
            assertTrue(ErrorCode.ACCT_NOT_FOUND.isError());
            assertEquals(8, ErrorCode.ACCT_NOT_FOUND.getSeverityCode());
            assertTrue(ErrorCode.CARD_NOT_FOUND.isError());
        }

        @Test
        @DisplayName("Input validation errors have severity 4 and isError() == true")
        void inputErrors_severity4() {
            assertTrue(ErrorCode.INPUT_REQUIRED.isError());
            assertEquals(4, ErrorCode.INPUT_REQUIRED.getSeverityCode());
        }

        @ParameterizedTest(name = "All ErrorCode instances have non-null fields: {0}")
        @EnumSource(ErrorCode.class)
        void allErrorCodes_haveNonNullFields(ErrorCode ec) {
            assertNotNull(ec.getMessageCode(),  ec.name() + " messageCode is null");
            assertNotNull(ec.getDescription(),  ec.name() + " description is null");
            assertTrue(ec.getSeverityCode() >= 0, ec.name() + " severity is negative");
        }
    }

    // =========================================================================
    // 2. CSMSG01Y.cpy – common message literals
    // =========================================================================

    @Nested
    @DisplayName("Common message literals (CSMSG01Y.cpy)")
    class CommonMessageLiteralsTests {

        @Test
        @DisplayName("MSG_THANK_YOU matches CCDA-MSG-THANK-YOU (50 chars)")
        void thankYouMessage_is50Chars() {
            assertEquals(50, MessageService.MSG_THANK_YOU.length());
            assertTrue(MessageService.MSG_THANK_YOU.contains("CardDemo"));
        }

        @Test
        @DisplayName("MSG_INVALID_KEY matches CCDA-MSG-INVALID-KEY (50 chars)")
        void invalidKeyMessage_is50Chars() {
            assertEquals(50, MessageService.MSG_INVALID_KEY.length());
            assertTrue(MessageService.MSG_INVALID_KEY.contains("Invalid key"));
        }
    }

    // =========================================================================
    // 3. buildDateValidationMessage() – WS-MESSAGE layout from CSUTLDTC.cbl
    // =========================================================================

    @Nested
    @DisplayName("buildDateValidationMessage() – WS-MESSAGE structure")
    class DateValidationMessageTests {

        @Test
        @DisplayName("Contains severity, message code, result, date, and mask fields")
        void dateValidationMessage_containsAllFields() {
            String msg = service.buildDateValidationMessage(
                    ErrorCode.DATE_BAD_VALUE, "20231301", "YYYYMMDD");
            assertTrue(msg.contains("Mesg Code:"));
            assertTrue(msg.contains("09CC"));          // messageCode
            assertTrue(msg.contains("TstDate:"));
            assertTrue(msg.contains("20231301"));
            assertTrue(msg.contains("Mask used:"));
            assertTrue(msg.contains("YYYYMMDD"));
        }

        @Test
        @DisplayName("Valid date produces severity 0 in message")
        void validDate_severity0InMessage() {
            String msg = service.buildDateValidationMessage(
                    ErrorCode.DATE_VALID, "20231231", "YYYYMMDD");
            assertTrue(msg.startsWith("0 ") || msg.contains("   0"));
        }

        @Test
        @DisplayName("Message length is exactly 80 characters")
        void dateValidationMessage_is80Chars() {
            String msg = service.buildDateValidationMessage(
                    ErrorCode.DATE_VALID, "20231231", "YYYYMMDD");
            assertEquals(80, msg.length());
        }
    }

    // =========================================================================
    // 4. buildFieldErrorMessage() – WS-RETURN-MSG composition
    // =========================================================================

    @Nested
    @DisplayName("buildFieldErrorMessage() – WS-RETURN-MSG STRING pattern")
    class FieldErrorMessageTests {

        @Test
        @DisplayName("Combines field name and error description")
        void fieldError_combinesNameAndDescription() {
            String msg = service.buildFieldErrorMessage("DOB", ErrorCode.DATE_FUTURE_DOB);
            assertTrue(msg.contains("DOB"));
            assertTrue(msg.contains("future"));
        }

        @Test
        @DisplayName("Does not exceed 80 characters (WS-RETURN-MSG PIC X(80))")
        void fieldError_maxLength80() {
            String longField = "A".repeat(100);
            String msg = service.buildFieldErrorMessage(longField, ErrorCode.DATE_BAD_VALUE);
            assertTrue(msg.length() <= 80);
        }

        @Test
        @DisplayName("Null field name handled gracefully")
        void fieldError_nullFieldName() {
            assertDoesNotThrow(() ->
                    service.buildFieldErrorMessage(null, ErrorCode.INPUT_REQUIRED));
        }
    }

    // =========================================================================
    // 5. buildAbendData() – CSMSG02Y.cpy ABEND-DATA
    // =========================================================================

    @Nested
    @DisplayName("buildAbendData() – ABEND-DATA structure (CSMSG02Y.cpy)")
    class AbendDataTests {

        @Test
        @DisplayName("AbendData fields match PIC X widths (4/8/50/72)")
        void abendData_fieldWidths() {
            AbendData ad = service.buildAbendData("AICA", "COACTUPC",
                    "File not found", "VSAM OPEN error on ACCTDAT");
            assertEquals(4,  ad.abendCode().length());
            assertEquals(8,  ad.culprit().length());
            assertEquals(50, ad.reason().length());
            assertEquals(72, ad.message().length());
        }

        @Test
        @DisplayName("Short fields are right-padded with spaces")
        void abendData_shortFieldsPadded() {
            AbendData ad = service.buildAbendData("A", "B", "C", "D");
            assertEquals(4,  ad.abendCode().length());
            assertEquals(8,  ad.culprit().length());
            assertEquals(50, ad.reason().length());
            assertEquals(72, ad.message().length());
            assertEquals("A   ", ad.abendCode());
        }

        @Test
        @DisplayName("toString concatenates all fields (total 134 chars)")
        void abendData_toStringLength() {
            AbendData ad = service.buildAbendData("A001", "COSGN00C",
                    "Auth failed", "User not found in USRSDAT");
            assertEquals(4 + 8 + 50 + 72, ad.toString().length());
        }
    }

    // =========================================================================
    // 6. raise() – ApplicationException throwing
    // =========================================================================

    @Nested
    @DisplayName("raise() – COBOL PERFORM 9000-ABEND / RETURN-CODE equivalent")
    class RaiseTests {

        @Test
        @DisplayName("raise() throws ApplicationException with correct ErrorCode")
        void raise_throwsApplicationException() {
            ApplicationException ex = assertThrows(ApplicationException.class,
                    () -> service.raise(ErrorCode.ACCT_NOT_FOUND, "accountId=12345"));
            assertEquals(ErrorCode.ACCT_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("raise() with context includes context in message")
        void raise_messageContainsContext() {
            ApplicationException ex = assertThrows(ApplicationException.class,
                    () -> service.raise(ErrorCode.DATE_BAD_VALUE, "testContext"));
            assertTrue(ex.getMessage().contains("testContext"));
        }

        @Test
        @DisplayName("raise() without context uses error description")
        void raise_noContext_usesDescription() {
            ApplicationException ex = assertThrows(ApplicationException.class,
                    () -> service.raise(ErrorCode.USER_INVALID_PWD));
            assertEquals(ErrorCode.USER_INVALID_PWD.getDescription(), ex.getMessage());
        }
    }

    // =========================================================================
    // 7. fromMessageCode() lookup
    // =========================================================================

    @Nested
    @DisplayName("fromMessageCode() – reverse lookup by COBOL message code")
    class FromMessageCodeTests {

        @Test
        @DisplayName("Looks up DATE_INSUFFICIENT by COBOL message code 09CB")
        void lookupByCode_dateInsufficient() {
            assertEquals(ErrorCode.DATE_INSUFFICIENT,
                    service.fromMessageCode("09CB"));
        }

        @Test
        @DisplayName("Case-insensitive lookup")
        void lookupByCode_caseInsensitive() {
            assertEquals(ErrorCode.DATE_BAD_VALUE,
                    service.fromMessageCode("09cc"));
        }

        @Test
        @DisplayName("Unknown code returns DATE_INVALID as default")
        void unknownCode_returnsDefault() {
            assertEquals(ErrorCode.DATE_INVALID,
                    service.fromMessageCode("XXXX"));
        }

        @Test
        @DisplayName("Null code returns DATE_INVALID as default")
        void nullCode_returnsDefault() {
            assertEquals(ErrorCode.DATE_INVALID,
                    service.fromMessageCode(null));
        }
    }

    // =========================================================================
    // 8. ApplicationException behaviour
    // =========================================================================

    @Nested
    @DisplayName("ApplicationException construction")
    class ApplicationExceptionTests {

        @Test
        @DisplayName("Constructor sets errorCode and context")
        void constructor_setsFields() {
            ApplicationException ex =
                    new ApplicationException(ErrorCode.CARD_INACTIVE, "card=4444333322221111");
            assertEquals(ErrorCode.CARD_INACTIVE, ex.getErrorCode());
            assertEquals("card=4444333322221111", ex.getContext());
        }

        @Test
        @DisplayName("isAbend() true for severity 12 ErrorCode")
        void isAbend_trueFor_severity12() {
            ApplicationException ex =
                    new ApplicationException(ErrorCode.ABEND_CICS);
            assertTrue(ex.getErrorCode().isAbend());
        }

        @Test
        @DisplayName("No-context constructor sets empty context")
        void noContextConstructor_emptyContext() {
            ApplicationException ex =
                    new ApplicationException(ErrorCode.TXN_DUPLICATE);
            assertEquals("", ex.getContext());
            assertEquals(ErrorCode.TXN_DUPLICATE.getDescription(), ex.getMessage());
        }
    }
}
