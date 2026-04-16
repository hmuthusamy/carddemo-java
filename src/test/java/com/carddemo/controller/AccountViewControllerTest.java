package com.carddemo.controller;

import com.carddemo.exception.AccountNotFoundException;
import com.carddemo.model.AccountViewResponse;
import com.carddemo.service.AccountViewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.carddemo.config.SecurityConfig;

/**
 * AccountViewControllerTest – @WebMvcTest slice for {@link AccountViewController}.
 *
 * Tests correspond to the COBOL COACTVWC behaviour paths:
 *
 * TC-01  Happy path – account exists       → HTTP 200 with full JSON body
 * TC-02  Account not in CXACAIX xref file  → HTTP 404
 * TC-03  Account not in ACCTDAT master     → HTTP 404
 * TC-04  Customer not in CUSTDAT master    → HTTP 404
 * TC-05  Account id is zero (invalid)      → HTTP 400
 * TC-06  Account id is negative (invalid)  → HTTP 400 / 404
 * TC-07  SSN formatted NNN-NN-NNNN         → HTTP 200, SSN field correct
 * TC-08  Verify all account fields present → HTTP 200, spot-check JSON fields
 */
@WebMvcTest(AccountViewController.class)
@Import(SecurityConfig.class)
class AccountViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountViewService accountViewService;

    private AccountViewResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleResponse = AccountViewResponse.builder()
                .accountId(12345678901L)
                .activeStatus("Y")
                .currentBalance(new BigDecimal("1500.75"))
                .creditLimit(new BigDecimal("5000.00"))
                .cashCreditLimit(new BigDecimal("1000.00"))
                .currCycCredit(new BigDecimal("200.00"))
                .currCycDebit(new BigDecimal("300.50"))
                .openDate("2020-01-15")
                .expirationDate("2025-01-15")
                .reissueDate("2023-01-15")
                .groupId("PREMIUM")
                .customerId(987654321L)
                .ssn("123-45-6789")
                .ficoCreditScore(750)
                .dateOfBirth("1985-06-20")
                .firstName("John")
                .middleName("Michael")
                .lastName("Doe")
                .addressLine1("123 Main St")
                .addressLine2("Apt 4B")
                .city("Springfield")
                .state("IL")
                .zipCode("62701")
                .country("USA")
                .phoneNumber1("2175551234")
                .phoneNumber2("2175555678")
                .govtIssuedId("DL-IL-12345678")
                .eftAccountId("EFT001")
                .primaryCardHolderFlag("Y")
                .build();
    }

    // ── TC-01: Happy path ─────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-01: GET /api/accounts/{id} returns 200 with account JSON when account exists")
    void tc01_getAccountView_existingAccount_returns200() throws Exception {
        when(accountViewService.getAccountView(12345678901L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/accounts/12345678901")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accountId",   is(12345678901L), Long.class))
                .andExpect(jsonPath("$.activeStatus", is("Y")))
                .andExpect(jsonPath("$.firstName",   is("John")))
                .andExpect(jsonPath("$.lastName",    is("Doe")));

        verify(accountViewService, times(1)).getAccountView(12345678901L);
    }

    // ── TC-02: Account not found in card xref ─────────────────────────────────
    @Test
    @DisplayName("TC-02: GET /api/accounts/{id} returns 404 when account not in card xref file")
    void tc02_getAccountView_notInCardXref_returns404() throws Exception {
        Long missingId = 99999999999L;
        when(accountViewService.getAccountView(missingId))
                .thenThrow(new AccountNotFoundException(missingId,
                        "Did not find this account in account card xref file"));

        mockMvc.perform(get("/api/accounts/99999999999")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error",   is("Account not found")))
                .andExpect(jsonPath("$.accountId", is(99999999999L), Long.class));
    }

    // ── TC-03: Account not found in account master ────────────────────────────
    @Test
    @DisplayName("TC-03: GET /api/accounts/{id} returns 404 when account not in ACCTDAT master")
    void tc03_getAccountView_notInAcctMaster_returns404() throws Exception {
        Long missingId = 11111111111L;
        when(accountViewService.getAccountView(missingId))
                .thenThrow(new AccountNotFoundException(missingId,
                        "Did not find this account in account master file"));

        mockMvc.perform(get("/api/accounts/11111111111")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error",   is("Account not found")))
                .andExpect(jsonPath("$.message", containsString("account master file")));
    }

    // ── TC-04: Customer not found in customer master ───────────────────────────
    @Test
    @DisplayName("TC-04: GET /api/accounts/{id} returns 404 when associated customer not found")
    void tc04_getAccountView_customerNotFound_returns404() throws Exception {
        Long accountId = 22222222222L;
        when(accountViewService.getAccountView(accountId))
                .thenThrow(new AccountNotFoundException(accountId,
                        "Did not find associated customer in master file"));

        mockMvc.perform(get("/api/accounts/22222222222")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("customer in master file")));
    }

    // ── TC-05: Zero account id is invalid ────────────────────────────────────
    @Test
    @DisplayName("TC-05: GET /api/accounts/0 returns 400 – mirrors SEARCHED-ACCT-ZEROES")
    void tc05_getAccountView_zeroAccountId_returns400() throws Exception {
        when(accountViewService.getAccountView(0L))
                .thenThrow(new IllegalArgumentException(
                        "Account number must be a non zero 11 digit number"));

        mockMvc.perform(get("/api/accounts/0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error",   is("Invalid account id")))
                .andExpect(jsonPath("$.message", containsString("non zero 11 digit")));
    }

    // ── TC-06: Negative account id treated as bad request ────────────────────
    @Test
    @DisplayName("TC-06: GET /api/accounts/-1 returns 400 for negative account id")
    void tc06_getAccountView_negativeAccountId_returns400() throws Exception {
        when(accountViewService.getAccountView(-1L))
                .thenThrow(new IllegalArgumentException(
                        "Account number must be a non zero 11 digit number"));

        mockMvc.perform(get("/api/accounts/-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Invalid account id")));
    }

    // ── TC-07: SSN formatted NNN-NN-NNNN ─────────────────────────────────────
    @Test
    @DisplayName("TC-07: GET /api/accounts/{id} returns SSN formatted NNN-NN-NNNN (mirrors COBOL STRING)")
    void tc07_getAccountView_ssnFormattedCorrectly() throws Exception {
        when(accountViewService.getAccountView(12345678901L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/accounts/12345678901")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ssn", matchesPattern("\\d{3}-\\d{2}-\\d{4}")));
    }

    // ── TC-08: All account financial fields present in response ───────────────
    @Test
    @DisplayName("TC-08: GET /api/accounts/{id} response body contains all account financial fields")
    void tc08_getAccountView_allFieldsPresent() throws Exception {
        when(accountViewService.getAccountView(12345678901L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/accounts/12345678901")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // Account financial fields (FOUND-ACCT-IN-MASTER branch)
                .andExpect(jsonPath("$.currentBalance",   notNullValue()))
                .andExpect(jsonPath("$.creditLimit",      notNullValue()))
                .andExpect(jsonPath("$.cashCreditLimit",  notNullValue()))
                .andExpect(jsonPath("$.currCycCredit",    notNullValue()))
                .andExpect(jsonPath("$.currCycDebit",     notNullValue()))
                .andExpect(jsonPath("$.openDate",         is("2020-01-15")))
                .andExpect(jsonPath("$.expirationDate",   is("2025-01-15")))
                .andExpect(jsonPath("$.groupId",          is("PREMIUM")))
                // Customer fields (FOUND-CUST-IN-MASTER branch)
                .andExpect(jsonPath("$.customerId",       notNullValue()))
                .andExpect(jsonPath("$.ficoCreditScore",  is(750)))
                .andExpect(jsonPath("$.addressLine1",     is("123 Main St")))
                .andExpect(jsonPath("$.state",            is("IL")))
                .andExpect(jsonPath("$.primaryCardHolderFlag", is("Y")));
    }
}
