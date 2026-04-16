package com.carddemo.controller;

import com.carddemo.model.TransactionAddRequest;
import com.carddemo.model.TransactionAddResponse;
import com.carddemo.service.TransactionAddService;
import com.carddemo.service.TransactionAddService.TransactionValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TransactionAddControllerTest – unit tests for the COTRNADD → Spring Boot migration.
 *
 * <p>Tests verify the REST layer independently from the service and database,
 * mirroring the original COBOL screen-flow scenarios:
 * <ul>
 *   <li>Successful add → HTTP 201 + transaction ID in body</li>
 *   <li>Validation failure (WS-ERR-FLG = 'Y') → HTTP 400 + error message</li>
 *   <li>Unexpected system error → HTTP 500 + generic message</li>
 * </ul>
 */
@WebMvcTest(TransactionAddController.class)
@DisplayName("COTRNADD – TransactionAddController tests")
class TransactionAddControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionAddService transactionAddService;

    // ================================================================== helpers

    /** Builds a fully populated, valid request (all COTRN2AI fields present). */
    private TransactionAddRequest validRequest() {
        return TransactionAddRequest.builder()
                .accountId("00000001001")
                .cardNumber(null)                       // service resolves via CCXREF
                .typeCode("01")
                .categoryCode("0001")
                .source("POS       ")
                .description("Purchase at test merchant")
                .amount(new BigDecimal("-00125.99"))
                .origDate("2024-01-15")
                .procDate("2024-01-16")
                .merchantId("000000001")
                .merchantName("Test Merchant Name")
                .merchantCity("Austin")
                .merchantZip("78701")
                .build();
    }

    /** Builds the expected response for a successful add. */
    private TransactionAddResponse successResponse() {
        return TransactionAddResponse.builder()
                .transactionId("0000000000000042")
                .cardNumber("4111111111111111")
                .accountId("00000001001")
                .typeCode("01")
                .categoryCode("0001")
                .source("POS")
                .description("Purchase at test merchant")
                .amount(new BigDecimal("-125.99"))
                .origDate("2024-01-15")
                .procDate("2024-01-16")
                .merchantId("000000001")
                .merchantName("Test Merchant Name")
                .merchantCity("Austin")
                .merchantZip("78701")
                // COBOL: "Transaction added successfully. Your Tran ID is <ID>."
                .message("Transaction added successfully. Your Tran ID is 0000000000000042.")
                .build();
    }

    // ================================================================== happy-path tests

    @Test
    @WithMockUser
    @DisplayName("POST /api/transactions – success → 201 Created with transaction ID")
    void addTransaction_validRequest_returns201() throws Exception {
        when(transactionAddService.addTransaction(any(TransactionAddRequest.class)))
                .thenReturn(successResponse());

        mockMvc.perform(post("/api/transactions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                // TRAN-ID generated and returned in body
                .andExpect(jsonPath("$.transactionId").value("0000000000000042"))
                .andExpect(jsonPath("$.cardNumber").value("4111111111111111"))
                .andExpect(jsonPath("$.accountId").value("00000001001"))
                .andExpect(jsonPath("$.typeCode").value("01"))
                .andExpect(jsonPath("$.categoryCode").value("0001"))
                .andExpect(jsonPath("$.amount").value(-125.99))
                .andExpect(jsonPath("$.origDate").value("2024-01-15"))
                .andExpect(jsonPath("$.procDate").value("2024-01-16"))
                .andExpect(jsonPath("$.merchantId").value("000000001"))
                .andExpect(jsonPath("$.merchantName").value("Test Merchant Name"))
                .andExpect(jsonPath("$.merchantCity").value("Austin"))
                .andExpect(jsonPath("$.merchantZip").value("78701"))
                // COBOL success message preserved
                .andExpect(jsonPath("$.message").value(
                        "Transaction added successfully. Your Tran ID is 0000000000000042."));

        verify(transactionAddService).addTransaction(any(TransactionAddRequest.class));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/transactions – card number supplied instead of account → 201 Created")
    void addTransaction_cardNumberSupplied_returns201() throws Exception {
        TransactionAddRequest req = validRequest();
        req.setAccountId(null);
        req.setCardNumber("4111111111111111");

        when(transactionAddService.addTransaction(any(TransactionAddRequest.class)))
                .thenReturn(successResponse());

        mockMvc.perform(post("/api/transactions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value("0000000000000042"));
    }

    // ================================================================== validation-error tests

    @Test
    @WithMockUser
    @DisplayName("POST /api/transactions – neither account nor card → 400 (COBOL: 'Account or Card Number must be entered')")
    void addTransaction_noAccountOrCard_returns400() throws Exception {
        when(transactionAddService.addTransaction(any(TransactionAddRequest.class)))
                .thenThrow(new TransactionValidationException(
                        "Account or Card Number must be entered..."));

        TransactionAddRequest req = validRequest();
        req.setAccountId(null);
        req.setCardNumber(null);

        mockMvc.perform(post("/api/transactions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        "Account or Card Number must be entered..."));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/transactions – non-numeric account ID → 400 (COBOL: 'Account ID must be Numeric')")
    void addTransaction_nonNumericAccountId_returns400() throws Exception {
        when(transactionAddService.addTransaction(any(TransactionAddRequest.class)))
                .thenThrow(new TransactionValidationException(
                        "Account ID must be Numeric..."));

        TransactionAddRequest req = validRequest();
        req.setAccountId("ABC123");

        mockMvc.perform(post("/api/transactions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Account ID must be Numeric..."));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/transactions – account not found in XREF → 400 (COBOL: 'Account ID NOT found')")
    void addTransaction_accountNotFound_returns400() throws Exception {
        when(transactionAddService.addTransaction(any(TransactionAddRequest.class)))
                .thenThrow(new TransactionValidationException("Account ID NOT found..."));

        mockMvc.perform(post("/api/transactions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Account ID NOT found..."));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/transactions – card not found in XREF → 400 (COBOL: 'Card Number NOT found')")
    void addTransaction_cardNotFound_returns400() throws Exception {
        when(transactionAddService.addTransaction(any(TransactionAddRequest.class)))
                .thenThrow(new TransactionValidationException("Card Number NOT found..."));

        TransactionAddRequest req = validRequest();
        req.setAccountId(null);
        req.setCardNumber("9999999999999999");

        mockMvc.perform(post("/api/transactions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Card Number NOT found..."));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/transactions – empty type code → 400 (COBOL: 'Type CD can NOT be empty')")
    void addTransaction_emptyTypeCode_returns400() throws Exception {
        when(transactionAddService.addTransaction(any(TransactionAddRequest.class)))
                .thenThrow(new TransactionValidationException("Type CD can NOT be empty..."));

        TransactionAddRequest req = validRequest();
        req.setTypeCode("");

        mockMvc.perform(post("/api/transactions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Type CD can NOT be empty..."));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/transactions – non-numeric type code → 400 (COBOL: 'Type CD must be Numeric')")
    void addTransaction_nonNumericTypeCode_returns400() throws Exception {
        when(transactionAddService.addTransaction(any(TransactionAddRequest.class)))
                .thenThrow(new TransactionValidationException("Type CD must be Numeric..."));

        TransactionAddRequest req = validRequest();
        req.setTypeCode("AB");

        mockMvc.perform(post("/api/transactions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Type CD must be Numeric..."));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/transactions – invalid orig date format → 400 (COBOL: 'Orig Date should be in format YYYY-MM-DD')")
    void addTransaction_invalidOrigDate_returns400() throws Exception {
        when(transactionAddService.addTransaction(any(TransactionAddRequest.class)))
                .thenThrow(new TransactionValidationException(
                        "Orig Date should be in format YYYY-MM-DD"));

        TransactionAddRequest req = validRequest();
        req.setOrigDate("15/01/2024");   // wrong format

        mockMvc.perform(post("/api/transactions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        "Orig Date should be in format YYYY-MM-DD"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/transactions – empty amount → 400 (COBOL: 'Amount can NOT be empty')")
    void addTransaction_nullAmount_returns400() throws Exception {
        when(transactionAddService.addTransaction(any(TransactionAddRequest.class)))
                .thenThrow(new TransactionValidationException("Amount can NOT be empty..."));

        TransactionAddRequest req = validRequest();
        req.setAmount(null);

        mockMvc.perform(post("/api/transactions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Amount can NOT be empty..."));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/transactions – non-numeric merchant ID → 400 (COBOL: 'Merchant ID must be Numeric')")
    void addTransaction_nonNumericMerchantId_returns400() throws Exception {
        when(transactionAddService.addTransaction(any(TransactionAddRequest.class)))
                .thenThrow(new TransactionValidationException(
                        "Merchant ID must be Numeric..."));

        TransactionAddRequest req = validRequest();
        req.setMerchantId("MERCH001");

        mockMvc.perform(post("/api/transactions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Merchant ID must be Numeric..."));
    }

    // ================================================================== system-error tests

    @Test
    @WithMockUser
    @DisplayName("POST /api/transactions – unexpected error → 500 (COBOL: DFHRESP OTHER → 'Unable to Add Transaction')")
    void addTransaction_unexpectedError_returns500() throws Exception {
        when(transactionAddService.addTransaction(any(TransactionAddRequest.class)))
                .thenThrow(new RuntimeException("DB connection lost"));

        mockMvc.perform(post("/api/transactions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Unable to Add Transaction..."));
    }
}
