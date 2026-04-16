package com.carddemo.controller;

import com.carddemo.model.AccountData;
import com.carddemo.model.AccountUpdateRequest;
import com.carddemo.model.AccountUpdateResponse;
import com.carddemo.repository.AccountDataRepository;
import com.carddemo.service.AccountUpdateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AccountUpdateControllerTest – unit tests for the COACTUPD migration.
 *
 * Tests cover:
 *  - HTTP layer (MockMvc) for PUT /api/accounts/{accountId}
 *  - Service validation logic (AccountUpdateService)
 *  - COBOL paragraph equivalences (annotated in test names)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccountUpdateController – COACTUPD migration tests")
class AccountUpdateControllerTest {

    /* ── MockMvc / Jackson setup ─────────────────────────────────────────── */

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private AccountDataRepository accountDataRepository;

    @InjectMocks
    private AccountUpdateService accountUpdateService;

    private AccountUpdateController controller;

    @BeforeEach
    void setUp() {
        controller = new AccountUpdateController(accountUpdateService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    /* ── Test fixtures ───────────────────────────────────────────────────── */

    private static AccountData buildAccount(String id) {
        return AccountData.builder()
                .accountId(id)
                .activeStatus("Y")
                .currentBalance(new BigDecimal("1500.00"))
                .creditLimit(new BigDecimal("5000.00"))
                .cashCreditLimit(new BigDecimal("1000.00"))
                .openDate(LocalDate.of(2020, 1, 1))
                .expirationDate(LocalDate.of(2026, 12, 31))
                .reissueDate(LocalDate.of(2024, 1, 1))
                .currCycCredit(new BigDecimal("200.00"))
                .currCycDebit(new BigDecimal("150.00"))
                .addrZip("10001")
                .groupId("GOLD")
                .build();
    }

    private static AccountUpdateRequest buildRequest() {
        return AccountUpdateRequest.builder()
                .activeStatus("Y")
                .creditLimit(new BigDecimal("6000.00"))
                .cashCreditLimit(new BigDecimal("1500.00"))
                .expirationDate(LocalDate.of(2028, 12, 31))
                .reissueDate(LocalDate.of(2025, 6, 1))
                .addrZip("10002")
                .groupId("PLATINUM")
                .currCycCredit(new BigDecimal("300.00"))
                .currCycDebit(new BigDecimal("100.00"))
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HTTP layer tests (MockMvc)
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("HTTP layer – PUT /api/accounts/{accountId}")
    class HttpLayerTests {

        @Test
        @DisplayName("200 OK – successful update (COBOL: DFHRESP(NORMAL))")
        void updateAccount_success_returns200() throws Exception {
            String accountId = "00000001234";
            AccountData account = buildAccount(accountId);
            when(accountDataRepository.findById(accountId)).thenReturn(Optional.of(account));
            when(accountDataRepository.save(any(AccountData.class))).thenAnswer(inv -> inv.getArgument(0));

            AccountUpdateRequest req = buildRequest();

            mockMvc.perform(put("/api/accounts/" + accountId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.accountId").value(accountId))
                    .andExpect(jsonPath("$.message").value("Account updated successfully"));
        }

        @Test
        @DisplayName("404 Not Found – account does not exist (COBOL: DFHRESP(NOTFND))")
        void updateAccount_notFound_returns404() throws Exception {
            String accountId = "99999999999";
            when(accountDataRepository.findById(accountId)).thenReturn(Optional.empty());

            mockMvc.perform(put("/api/accounts/" + accountId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildRequest())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("400 Bad Request – invalid account ID (non-numeric)")
        void updateAccount_invalidAccountId_returns400() throws Exception {
            mockMvc.perform(put("/api/accounts/INVALID_ID")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildRequest())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("400 Bad Request – validation errors returned in errors array")
        void updateAccount_validationError_returns400WithErrors() throws Exception {
            String accountId = "00000001234";
            AccountData account = buildAccount(accountId);
            when(accountDataRepository.findById(accountId)).thenReturn(Optional.of(account));

            AccountUpdateRequest req = AccountUpdateRequest.builder()
                    .activeStatus("X")                         // invalid – not Y/N
                    .creditLimit(new BigDecimal("-100.00"))    // negative
                    .addrZip("ABCDEFGHIJK")                    // non-numeric and too long
                    .build();

            mockMvc.perform(put("/api/accounts/" + accountId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors").isArray());
        }

        @Test
        @DisplayName("500 Internal Server Error – repository throws unexpected exception")
        void updateAccount_unexpectedException_returns500() throws Exception {
            String accountId = "00000001234";
            when(accountDataRepository.findById(accountId)).thenThrow(new RuntimeException("DB connection lost"));

            mockMvc.perform(put("/api/accounts/" + accountId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildRequest())))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Service validation tests  (COBOL 8000-VALIDATE-INPUT and sub-paragraphs)
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Service – validateRequest (COBOL 8000-VALIDATE-INPUT)")
    class ServiceValidationTests {

        @Test
        @DisplayName("8100 – invalid active status 'X' produces error")
        void validateActiveStatus_invalidValue_addsError() {
            List<String> errors = new ArrayList<>();
            accountUpdateService.validateActiveStatus("X", errors);
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0)).contains("Active status must be");
        }

        @Test
        @DisplayName("8100 – valid active status 'Y' produces no error")
        void validateActiveStatus_Y_noError() {
            List<String> errors = new ArrayList<>();
            accountUpdateService.validateActiveStatus("Y", errors);
            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("8100 – valid active status 'N' produces no error")
        void validateActiveStatus_N_noError() {
            List<String> errors = new ArrayList<>();
            accountUpdateService.validateActiveStatus("N", errors);
            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("8100 – null active status is treated as 'not supplied' → no error")
        void validateActiveStatus_null_noError() {
            List<String> errors = new ArrayList<>();
            accountUpdateService.validateActiveStatus(null, errors);
            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("8200 – negative credit limit produces error")
        void validateCreditLimit_negative_addsError() {
            List<String> errors = new ArrayList<>();
            accountUpdateService.validateCreditLimit(new BigDecimal("-1.00"), errors);
            assertThat(errors).anyMatch(e -> e.contains("Credit limit must be >= 0"));
        }

        @Test
        @DisplayName("8200 – credit limit exceeding PIC 9(10)V99 max produces error")
        void validateCreditLimit_exceedsMax_addsError() {
            List<String> errors = new ArrayList<>();
            accountUpdateService.validateCreditLimit(new BigDecimal("10000000000.00"), errors);
            assertThat(errors).anyMatch(e -> e.contains("exceeds maximum"));
        }

        @Test
        @DisplayName("8200 – valid credit limit produces no error")
        void validateCreditLimit_valid_noError() {
            List<String> errors = new ArrayList<>();
            accountUpdateService.validateCreditLimit(new BigDecimal("5000.00"), errors);
            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("8300 – cash limit > credit limit produces error")
        void validateCashCreditLimit_greaterThanCredit_addsError() {
            List<String> errors = new ArrayList<>();
            accountUpdateService.validateCashCreditLimit(
                    new BigDecimal("6000.00"), new BigDecimal("5000.00"), errors);
            assertThat(errors).anyMatch(e -> e.contains("must not exceed credit limit"));
        }

        @Test
        @DisplayName("8300 – cash limit <= credit limit produces no error")
        void validateCashCreditLimit_lessThanCredit_noError() {
            List<String> errors = new ArrayList<>();
            accountUpdateService.validateCashCreditLimit(
                    new BigDecimal("1000.00"), new BigDecimal("5000.00"), errors);
            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("8400 – past expiration date produces error")
        void validateExpirationDate_past_addsError() {
            List<String> errors = new ArrayList<>();
            accountUpdateService.validateExpirationDate(LocalDate.of(2020, 1, 1), errors);
            assertThat(errors).anyMatch(e -> e.contains("must not be in the past"));
        }

        @Test
        @DisplayName("8400 – future expiration date produces no error")
        void validateExpirationDate_future_noError() {
            List<String> errors = new ArrayList<>();
            accountUpdateService.validateExpirationDate(LocalDate.now().plusYears(2), errors);
            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("8600 – non-numeric ZIP code produces error")
        void validateZipCode_nonNumeric_addsError() {
            List<String> errors = new ArrayList<>();
            accountUpdateService.validateZipCode("ABC123", errors);
            assertThat(errors).anyMatch(e -> e.contains("only digits"));
        }

        @Test
        @DisplayName("8600 – ZIP code exceeding 10 chars produces error")
        void validateZipCode_tooLong_addsError() {
            List<String> errors = new ArrayList<>();
            accountUpdateService.validateZipCode("12345678901", errors);
            assertThat(errors).anyMatch(e -> e.contains("at most 10 characters"));
        }

        @Test
        @DisplayName("8600 – valid 5-digit ZIP produces no error")
        void validateZipCode_valid_noError() {
            List<String> errors = new ArrayList<>();
            accountUpdateService.validateZipCode("10001", errors);
            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("8700 – group ID exceeding 10 chars produces error")
        void validateGroupId_tooLong_addsError() {
            List<String> errors = new ArrayList<>();
            accountUpdateService.validateGroupId("TOOLONGVALUE", errors);
            assertThat(errors).anyMatch(e -> e.contains("at most 10 characters"));
        }

        @Test
        @DisplayName("8700 – valid 10-char group ID produces no error")
        void validateGroupId_tenChars_noError() {
            List<String> errors = new ArrayList<>();
            accountUpdateService.validateGroupId("PLATINUMXX", errors);
            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("8800 – negative cycle credit produces error")
        void validateCycleAmounts_negativeCycCredit_addsError() {
            List<String> errors = new ArrayList<>();
            accountUpdateService.validateCycleAmounts(new BigDecimal("-50.00"), null, errors);
            assertThat(errors).anyMatch(e -> e.contains("cycle credit must be >= 0"));
        }

        @Test
        @DisplayName("8800 – negative cycle debit produces error")
        void validateCycleAmounts_negativeCycDebit_addsError() {
            List<String> errors = new ArrayList<>();
            accountUpdateService.validateCycleAmounts(null, new BigDecimal("-10.00"), errors);
            assertThat(errors).anyMatch(e -> e.contains("cycle debit must be >= 0"));
        }

        @Test
        @DisplayName("8800 – valid cycle amounts produce no errors")
        void validateCycleAmounts_valid_noError() {
            List<String> errors = new ArrayList<>();
            accountUpdateService.validateCycleAmounts(
                    new BigDecimal("200.00"), new BigDecimal("150.00"), errors);
            assertThat(errors).isEmpty();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Service – validateAccountId tests (COBOL 9000-VALIDATE-ACCT-ID)
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Service – validateAccountId (COBOL 9000-VALIDATE-ACCT-ID)")
    class AccountIdValidationTests {

        @Test
        @DisplayName("Non-numeric account ID produces error")
        void validateAccountId_nonNumeric_addsError() {
            List<String> errors = new ArrayList<>();
            accountUpdateService.validateAccountId("ABCDE123456", errors);
            assertThat(errors).anyMatch(e -> e.contains("only digits"));
        }

        @Test
        @DisplayName("Blank account ID produces error")
        void validateAccountId_blank_addsError() {
            List<String> errors = new ArrayList<>();
            accountUpdateService.validateAccountId("", errors);
            assertThat(errors).anyMatch(e -> e.contains("must not be blank"));
        }

        @Test
        @DisplayName("Account ID longer than 11 digits produces error")
        void validateAccountId_tooLong_addsError() {
            List<String> errors = new ArrayList<>();
            accountUpdateService.validateAccountId("123456789012", errors);  // 12 digits
            assertThat(errors).anyMatch(e -> e.contains("at most 11 digits"));
        }

        @Test
        @DisplayName("Valid 11-digit account ID produces no error")
        void validateAccountId_valid_noError() {
            List<String> errors = new ArrayList<>();
            accountUpdateService.validateAccountId("00000001234", errors);
            assertThat(errors).isEmpty();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Service – applyUpdates tests (COBOL MOVE screen fields → WS-ACCT-DATA)
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Service – applyUpdates (COBOL MOVE screen fields to WS-ACCT-DATA)")
    class ApplyUpdatesTests {

        @Test
        @DisplayName("Only non-null fields are applied – null fields unchanged")
        void applyUpdates_nullFieldsPreserveExisting() {
            AccountData account = buildAccount("00000001234");
            // Only update activeStatus; leave everything else null
            AccountUpdateRequest req = AccountUpdateRequest.builder()
                    .activeStatus("N")
                    .build();
            accountUpdateService.applyUpdates(account, req);

            assertThat(account.getActiveStatus()).isEqualTo("N");
            // All other fields should retain original values from buildAccount
            assertThat(account.getCreditLimit()).isEqualByComparingTo("5000.00");
            assertThat(account.getAddrZip()).isEqualTo("10001");
            assertThat(account.getGroupId()).isEqualTo("GOLD");
        }

        @Test
        @DisplayName("All fields updated when all provided")
        void applyUpdates_allFieldsUpdated() {
            AccountData account = buildAccount("00000001234");
            AccountUpdateRequest req = buildRequest();
            accountUpdateService.applyUpdates(account, req);

            assertThat(account.getActiveStatus()).isEqualTo("Y");
            assertThat(account.getCreditLimit()).isEqualByComparingTo("6000.00");
            assertThat(account.getCashCreditLimit()).isEqualByComparingTo("1500.00");
            assertThat(account.getExpirationDate()).isEqualTo(LocalDate.of(2028, 12, 31));
            assertThat(account.getReissueDate()).isEqualTo(LocalDate.of(2025, 6, 1));
            assertThat(account.getAddrZip()).isEqualTo("10002");
            assertThat(account.getGroupId()).isEqualTo("PLATINUM");
            assertThat(account.getCurrCycCredit()).isEqualByComparingTo("300.00");
            assertThat(account.getCurrCycDebit()).isEqualByComparingTo("100.00");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Service – full updateAccount integration-style tests
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Service – updateAccount end-to-end")
    class UpdateAccountEndToEndTests {

        @Test
        @DisplayName("Successful update returns success=true with entity snapshot")
        void updateAccount_success() {
            String id = "00000001234";
            AccountData account = buildAccount(id);
            when(accountDataRepository.findById(id)).thenReturn(Optional.of(account));
            when(accountDataRepository.save(any(AccountData.class))).thenAnswer(inv -> inv.getArgument(0));

            AccountUpdateResponse response = accountUpdateService.updateAccount(id, buildRequest());

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getAccountId()).isEqualTo(id);
            assertThat(response.getCreditLimit()).isEqualByComparingTo("6000.00");
            verify(accountDataRepository).save(any(AccountData.class));
        }

        @Test
        @DisplayName("Account not found returns success=false with 'not found' message")
        void updateAccount_notFound() {
            String id = "99999999999";
            when(accountDataRepository.findById(id)).thenReturn(Optional.empty());

            AccountUpdateResponse response = accountUpdateService.updateAccount(id, buildRequest());

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessage()).contains("not found");
            verify(accountDataRepository, never()).save(any());
        }

        @Test
        @DisplayName("Validation failure does NOT call repository.save()")
        void updateAccount_validationFails_noSave() {
            String id = "00000001234";
            AccountData account = buildAccount(id);
            when(accountDataRepository.findById(id)).thenReturn(Optional.of(account));

            AccountUpdateRequest badReq = AccountUpdateRequest.builder()
                    .activeStatus("INVALID")
                    .creditLimit(new BigDecimal("-500.00"))
                    .build();

            AccountUpdateResponse response = accountUpdateService.updateAccount(id, badReq);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getErrors()).isNotEmpty();
            verify(accountDataRepository, never()).save(any());
        }

        @Test
        @DisplayName("Invalid accountId format returns error without hitting repository")
        void updateAccount_invalidId_noRepositoryCall() {
            AccountUpdateResponse response =
                    accountUpdateService.updateAccount("NOT-NUMERIC!", buildRequest());

            assertThat(response.isSuccess()).isFalse();
            verify(accountDataRepository, never()).findById(any());
            verify(accountDataRepository, never()).save(any());
        }
    }
}
