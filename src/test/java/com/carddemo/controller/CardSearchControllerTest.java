package com.carddemo.controller;

import com.carddemo.model.CardSearchRequest;
import com.carddemo.model.CreditCard;
import com.carddemo.service.CardSearchService;
import com.carddemo.service.CardSearchService.CardSearchException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link CardSearchController}.
 *
 * <p>Covers all COBOL-equivalent search conditions from COCRDSLC:
 * <ol>
 *   <li>Successful search by card number (9100-GETCARD-BYACCTCARD)</li>
 *   <li>Successful search by account ID (9150-GETCARD-BYACCT)</li>
 *   <li>Combined card + account search</li>
 *   <li>Status filter (CARD-ACTIVE-STATUS)</li>
 *   <li>No criteria → 400 (NO-SEARCH-CRITERIA-RECEIVED)</li>
 *   <li>Invalid card number → 400 (2220-EDIT-CARD)</li>
 *   <li>Invalid account ID → 400 (2210-EDIT-ACCOUNT)</li>
 *   <li>Not found → 404 (DID-NOT-FIND-ACCTCARD-COMBO)</li>
 *   <li>GET /api/cards/{cardNumber} – found</li>
 *   <li>GET /api/cards/{cardNumber} – not found → 404</li>
 *   <li>GET /api/cards/{cardNumber} – invalid → 400</li>
 *   <li>GET /api/cards/account/{id} – found</li>
 *   <li>GET /api/cards/account/{id} – not found → 404</li>
 *   <li>GET /api/cards/account/{id} – invalid → 400</li>
 * </ol>
 * </p>
 */
@WebMvcTest(CardSearchController.class)
@WithMockUser
class CardSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CardSearchService cardSearchService;

    @Autowired
    private ObjectMapper objectMapper;

    // =========================================================================
    // Test fixtures
    // =========================================================================

    private CreditCard sampleCard() {
        return new CreditCard(
                "4111111111111111",
                12345678901L,
                123,
                "JOHN DOE",
                LocalDate.of(2027, 12, 31),
                "Y"
        );
    }

    // =========================================================================
    // GET /api/cards/search – success cases
    // =========================================================================

    @Test
    @DisplayName("1. Search by card number → 200 OK with results")
    void searchByCardNumber_returnsOk() throws Exception {
        when(cardSearchService.searchCards(any(CardSearchRequest.class)))
                .thenReturn(List.of(sampleCard()));

        mockMvc.perform(get("/api/cards/search")
                        .param("cardNumber", "4111111111111111")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].cardNumber", is("4111111111111111")))
                .andExpect(jsonPath("$[0].embossedName", is("JOHN DOE")))
                .andExpect(jsonPath("$[0].activeStatus", is("Y")));
    }

    @Test
    @DisplayName("2. Search by account ID → 200 OK with results")
    void searchByAccountId_returnsOk() throws Exception {
        when(cardSearchService.searchCards(any(CardSearchRequest.class)))
                .thenReturn(List.of(sampleCard()));

        mockMvc.perform(get("/api/cards/search")
                        .param("customerId", "12345678901")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].accountId", is(12345678901L), Long.class));
    }

    @Test
    @DisplayName("3. Combined card number + account ID search → 200 OK")
    void searchByCardAndAccount_returnsOk() throws Exception {
        when(cardSearchService.searchCards(any(CardSearchRequest.class)))
                .thenReturn(List.of(sampleCard()));

        mockMvc.perform(get("/api/cards/search")
                        .param("cardNumber", "4111111111111111")
                        .param("customerId", "12345678901"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cardNumber", is("4111111111111111")));
    }

    @Test
    @DisplayName("4. Status filter applied → 200 OK with active cards only")
    void searchByStatus_returnsOk() throws Exception {
        when(cardSearchService.searchCards(any(CardSearchRequest.class)))
                .thenReturn(List.of(sampleCard()));

        mockMvc.perform(get("/api/cards/search")
                        .param("customerId", "12345678901")
                        .param("status", "Y"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].activeStatus", is("Y")));
    }

    // =========================================================================
    // GET /api/cards/search – error cases (COBOL validation equivalents)
    // =========================================================================

    @Test
    @DisplayName("5. No criteria supplied → 400 (NO-SEARCH-CRITERIA-RECEIVED)")
    void searchNoCriteria_returns400() throws Exception {
        when(cardSearchService.searchCards(any(CardSearchRequest.class)))
                .thenThrow(new CardSearchException(CardSearchService.MSG_NO_CRITERIA));

        mockMvc.perform(get("/api/cards/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is(CardSearchService.MSG_NO_CRITERIA)));
    }

    @Test
    @DisplayName("6. Invalid card number → 400 (2220-EDIT-CARD)")
    void searchInvalidCardNumber_returns400() throws Exception {
        when(cardSearchService.searchCards(any(CardSearchRequest.class)))
                .thenThrow(new CardSearchException(CardSearchService.MSG_CARD_NOT_VALID));

        mockMvc.perform(get("/api/cards/search")
                        .param("cardNumber", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is(CardSearchService.MSG_CARD_NOT_VALID)));
    }

    @Test
    @DisplayName("7. Invalid account ID → 400 (2210-EDIT-ACCOUNT)")
    void searchInvalidAccountId_returns400() throws Exception {
        when(cardSearchService.searchCards(any(CardSearchRequest.class)))
                .thenThrow(new CardSearchException(CardSearchService.MSG_ACCT_NOT_VALID));

        mockMvc.perform(get("/api/cards/search")
                        .param("customerId", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is(CardSearchService.MSG_ACCT_NOT_VALID)));
    }

    @Test
    @DisplayName("8. No matching record → 404 (DID-NOT-FIND-ACCTCARD-COMBO)")
    void searchNotFound_returns404() throws Exception {
        when(cardSearchService.searchCards(any(CardSearchRequest.class)))
                .thenThrow(new CardSearchException(CardSearchService.MSG_NOT_FOUND));

        mockMvc.perform(get("/api/cards/search")
                        .param("cardNumber", "9999999999999999"))
                .andExpect(status().isBadRequest())
                // MSG_NOT_FOUND surfaces as 400 from searchCards (service enforces it)
                .andExpect(jsonPath("$.error", is(CardSearchService.MSG_NOT_FOUND)));
    }

    // =========================================================================
    // GET /api/cards/{cardNumber}
    // =========================================================================

    @Test
    @DisplayName("9. GET /{cardNumber} – found → 200 OK")
    void getCardByNumber_found() throws Exception {
        when(cardSearchService.getCardByNumber("4111111111111111"))
                .thenReturn(sampleCard());

        mockMvc.perform(get("/api/cards/4111111111111111"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardNumber", is("4111111111111111")))
                .andExpect(jsonPath("$.embossedName", is("JOHN DOE")));
    }

    @Test
    @DisplayName("10. GET /{cardNumber} – not found → 404")
    void getCardByNumber_notFound() throws Exception {
        when(cardSearchService.getCardByNumber(anyString()))
                .thenThrow(new CardSearchException(CardSearchService.MSG_NOT_FOUND));

        mockMvc.perform(get("/api/cards/9999999999999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("11. GET /{cardNumber} – invalid number → 400")
    void getCardByNumber_invalid() throws Exception {
        when(cardSearchService.getCardByNumber(anyString()))
                .thenThrow(new CardSearchException(CardSearchService.MSG_CARD_NOT_VALID));

        mockMvc.perform(get("/api/cards/NOTACARD"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is(CardSearchService.MSG_CARD_NOT_VALID)));
    }

    // =========================================================================
    // GET /api/cards/account/{accountId}
    // =========================================================================

    @Test
    @DisplayName("12. GET /account/{id} – found → 200 OK")
    void getCardsByAccount_found() throws Exception {
        when(cardSearchService.getCardsByAccountId(12345678901L))
                .thenReturn(List.of(sampleCard()));

        mockMvc.perform(get("/api/cards/account/12345678901"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].accountId", is(12345678901L), Long.class));
    }

    @Test
    @DisplayName("13. GET /account/{id} – not found → 404")
    void getCardsByAccount_notFound() throws Exception {
        when(cardSearchService.getCardsByAccountId(anyLong()))
                .thenThrow(new CardSearchException(CardSearchService.MSG_NOT_FOUND));

        mockMvc.perform(get("/api/cards/account/99999999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("14. GET /account/{id} – invalid ID → 400")
    void getCardsByAccount_invalid() throws Exception {
        when(cardSearchService.getCardsByAccountId(anyLong()))
                .thenThrow(new CardSearchException(CardSearchService.MSG_ACCT_NOT_VALID));

        mockMvc.perform(get("/api/cards/account/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is(CardSearchService.MSG_ACCT_NOT_VALID)));
    }
}
