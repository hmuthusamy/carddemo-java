package com.carddemo.controller;

import com.carddemo.exception.CardDataStaleException;
import com.carddemo.exception.CardNotFoundException;
import com.carddemo.model.CardData;
import com.carddemo.model.CardUpdateRequest;
import com.carddemo.model.CardUpdateResponse;
import com.carddemo.service.CardUpdateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link CardUpdateController}.
 *
 * Uses {@code @WebMvcTest} (Spring MVC slice) to verify:
 *   – HTTP routing and response codes
 *   – Bean Validation (paragraphs 1210–1260 equivalents)
 *   – Luhn check pass/fail path
 *   – Service exception → HTTP status mapping
 *   – Optimistic-lock conflict (9300-CHECK-CHANGE-IN-REC) → 409
 */
@WebMvcTest(CardUpdateController.class)
@WithMockUser
class CardUpdateControllerTest {

    private static final String VALID_CARD_NUMBER   = "4532015112830366"; // passes Luhn
    private static final String INVALID_LUHN_CARD   = "4532015112830367"; // fails Luhn
    private static final String VALID_ACCOUNT_ID    = "12345678901";
    private static final String BASE_URL            = "/api/cards/";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CardUpdateService cardUpdateService;

    private CardData sampleCard;
    private CardUpdateRequest validRequest;

    @BeforeEach
    void setUp() {
        sampleCard = CardData.builder()
                .cardNumber(VALID_CARD_NUMBER)
                .accountId(12345678901L)
                .cvvCode("123")
                .embossedName("JOHN DOE")
                .expirationDate("2027-12-01")
                .activeStatus("Y")
                .version(1L)
                .build();

        validRequest = CardUpdateRequest.builder()
                .accountId(VALID_ACCOUNT_ID)
                .embossedName("JOHN DOE")
                .expiryYear("2027")
                .expiryMonth("12")
                .expiryDay("01")
                .activeStatus("Y")
                .cvvCode("123")
                .build();
    }

    // =======================================================================
    // GET /api/cards/{cardNumber}
    // =======================================================================

    @Nested
    @DisplayName("GET /api/cards/{cardNumber} — 9100-GETCARD-BYACCTCARD")
    class GetCardTests {

        @Test
        @DisplayName("TC-01: returns 200 OK with card data when card exists")
        void getCard_found_returns200() throws Exception {
            when(cardUpdateService.getCard(VALID_CARD_NUMBER)).thenReturn(sampleCard);

            mockMvc.perform(get(BASE_URL + VALID_CARD_NUMBER))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.card.cardNumber").value(VALID_CARD_NUMBER))
                    .andExpect(jsonPath("$.card.embossedName").value("JOHN DOE"))
                    .andExpect(jsonPath("$.message").value("Details of selected card shown above"));
        }

        @Test
        @DisplayName("TC-02: returns 404 when card not found (DFHRESP NOTFND)")
        void getCard_notFound_returns404() throws Exception {
            when(cardUpdateService.getCard(VALID_CARD_NUMBER))
                    .thenThrow(new CardNotFoundException(VALID_CARD_NUMBER));

            mockMvc.perform(get(BASE_URL + VALID_CARD_NUMBER))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("TC-03: returns 400 when card number is not 16 digits")
        void getCard_badCardNumber_returns400() throws Exception {
            mockMvc.perform(get(BASE_URL + "1234"))
                    .andExpect(status().isBadRequest());
        }
    }

    // =======================================================================
    // PUT /api/cards/{cardNumber}
    // =======================================================================

    @Nested
    @DisplayName("PUT /api/cards/{cardNumber} — 9200-WRITE-PROCESSING")
    class UpdateCardTests {

        @Test
        @DisplayName("TC-04: successful update returns 200 OK (CCUP-CHANGES-OKAYED-AND-DONE)")
        void updateCard_success_returns200() throws Exception {
            CardUpdateResponse resp = CardUpdateResponse.builder()
                    .status(CardUpdateResponse.Status.SUCCESS)
                    .message("Changes committed to database")
                    .card(sampleCard)
                    .build();

            when(cardUpdateService.validateLuhn(VALID_CARD_NUMBER)).thenReturn(true);
            when(cardUpdateService.updateCard(eq(VALID_CARD_NUMBER), any())).thenReturn(resp);

            mockMvc.perform(put(BASE_URL + VALID_CARD_NUMBER)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").value("Changes committed to database"))
                    .andExpect(jsonPath("$.card.cardNumber").value(VALID_CARD_NUMBER));
        }

        @Test
        @DisplayName("TC-05: Luhn check failure returns 400 (1220-EDIT-CARD extended)")
        void updateCard_luhnFail_returns400() throws Exception {
            when(cardUpdateService.validateLuhn(INVALID_LUHN_CARD)).thenReturn(false);

            mockMvc.perform(put(BASE_URL + INVALID_LUHN_CARD)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value(containsString("Luhn")));

            verify(cardUpdateService, never()).updateCard(any(), any());
        }

        @Test
        @DisplayName("TC-06: card not found returns 404 (COULD-NOT-LOCK-FOR-UPDATE)")
        void updateCard_cardNotFound_returns404() throws Exception {
            when(cardUpdateService.validateLuhn(VALID_CARD_NUMBER)).thenReturn(true);
            when(cardUpdateService.updateCard(eq(VALID_CARD_NUMBER), any()))
                    .thenThrow(new CardNotFoundException(VALID_CARD_NUMBER));

            mockMvc.perform(put(BASE_URL + VALID_CARD_NUMBER)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("TC-07: stale data returns 409 (9300-CHECK-CHANGE-IN-REC)")
        void updateCard_staleData_returns409() throws Exception {
            when(cardUpdateService.validateLuhn(VALID_CARD_NUMBER)).thenReturn(true);
            when(cardUpdateService.updateCard(eq(VALID_CARD_NUMBER), any()))
                    .thenThrow(new CardDataStaleException(VALID_CARD_NUMBER));

            mockMvc.perform(put(BASE_URL + VALID_CARD_NUMBER)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value("CONFLICT"))
                    .andExpect(jsonPath("$.message")
                            .value(containsString("Record changed by someone else")));
        }

        // -------------------------------------------------------------------
        // Bean Validation tests (paragraphs 1210–1260 equivalents)
        // -------------------------------------------------------------------

        @Test
        @DisplayName("TC-08: missing accountId returns 400 (1210-EDIT-ACCOUNT blank)")
        void updateCard_missingAccountId_returns400() throws Exception {
            validRequest.setAccountId(null);

            mockMvc.perform(put(BASE_URL + VALID_CARD_NUMBER)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("TC-09: non-numeric accountId returns 400 (1210-EDIT-ACCOUNT not-numeric)")
        void updateCard_nonNumericAccountId_returns400() throws Exception {
            validRequest.setAccountId("ABCDEFGHIJK");

            mockMvc.perform(put(BASE_URL + VALID_CARD_NUMBER)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("TC-10: missing embossedName returns 400 (1230-EDIT-NAME blank)")
        void updateCard_missingEmbossedName_returns400() throws Exception {
            validRequest.setEmbossedName(null);

            mockMvc.perform(put(BASE_URL + VALID_CARD_NUMBER)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("TC-11: numeric embossedName returns 400 (1230-EDIT-NAME alpha only)")
        void updateCard_numericEmbossedName_returns400() throws Exception {
            validRequest.setEmbossedName("JOHN123DOE");

            mockMvc.perform(put(BASE_URL + VALID_CARD_NUMBER)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("TC-12: invalid activeStatus returns 400 (1240-EDIT-CARDSTATUS)")
        void updateCard_invalidActiveStatus_returns400() throws Exception {
            validRequest.setActiveStatus("X");

            mockMvc.perform(put(BASE_URL + VALID_CARD_NUMBER)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("TC-13: missing activeStatus returns 400 (1240-EDIT-CARDSTATUS blank)")
        void updateCard_missingActiveStatus_returns400() throws Exception {
            validRequest.setActiveStatus(null);

            mockMvc.perform(put(BASE_URL + VALID_CARD_NUMBER)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("TC-14: invalid expiryMonth returns 400 (1250-EDIT-EXPIRY-MON)")
        void updateCard_invalidExpiryMonth_returns400() throws Exception {
            validRequest.setExpiryMonth("13");

            mockMvc.perform(put(BASE_URL + VALID_CARD_NUMBER)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("TC-15: missing expiryMonth returns 400 (1250-EDIT-EXPIRY-MON blank)")
        void updateCard_missingExpiryMonth_returns400() throws Exception {
            validRequest.setExpiryMonth(null);

            mockMvc.perform(put(BASE_URL + VALID_CARD_NUMBER)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("TC-16: out-of-range expiryYear returns 400 (1260-EDIT-EXPIRY-YEAR VALID-YEAR)")
        void updateCard_outOfRangeExpiryYear_returns400() throws Exception {
            validRequest.setExpiryYear("1900");

            mockMvc.perform(put(BASE_URL + VALID_CARD_NUMBER)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("TC-17: missing expiryYear returns 400 (1260-EDIT-EXPIRY-YEAR blank)")
        void updateCard_missingExpiryYear_returns400() throws Exception {
            validRequest.setExpiryYear(null);

            mockMvc.perform(put(BASE_URL + VALID_CARD_NUMBER)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("TC-18: invalid CVV format returns 400")
        void updateCard_invalidCvv_returns400() throws Exception {
            validRequest.setCvvCode("12X");

            mockMvc.perform(put(BASE_URL + VALID_CARD_NUMBER)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("TC-19: card number not 16 digits returns 400 (1220-EDIT-CARD)")
        void updateCard_badCardNumberLength_returns400() throws Exception {
            mockMvc.perform(put(BASE_URL + "123456789")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest());
        }
    }
}
