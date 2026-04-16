package com.carddemo.controller;

import com.carddemo.model.CardData;
import com.carddemo.service.CardListService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CardListControllerTest – unit tests for {@link CardListController}.
 *
 * <p>Uses {@link WebMvcTest} to load only the web layer (no DB, no full context).
 * The {@link CardListService} is mocked via {@link MockBean}.
 *
 * <h2>Test scenarios vs COBOL logic</h2>
 * <table border="1">
 *   <tr><th>Test</th><th>COBOL equivalent</th></tr>
 *   <tr>
 *     <td>listCards_withAccountId_returnsFilteredPage</td>
 *     <td>9500-FILTER-RECORDS with FLG-ACCTFILTER-ISVALID; 9000-READ-FORWARD for user</td>
 *   </tr>
 *   <tr>
 *     <td>listCards_adminNoAccountId_returnsAllCards</td>
 *     <td>9000-READ-FORWARD with no account filter (admin path)</td>
 *   </tr>
 *   <tr>
 *     <td>listCards_withCursor_usesGteqBrowse</td>
 *     <td>PF8 page-down: MOVE WS-CA-LAST-CARD-NUM TO WS-CARD-RID-CARDNUM; STARTBR GTEQ</td>
 *   </tr>
 *   <tr>
 *     <td>listCards_pageDownHasNextPage_flagsNextPageExists</td>
 *     <td>CA-NEXT-PAGE-EXISTS flag set when more records follow</td>
 *   </tr>
 *   <tr>
 *     <td>listCards_firstPage_noPrevPage</td>
 *     <td>CA-FIRST-PAGE → PF7 disabled</td>
 *   </tr>
 *   <tr>
 *     <td>listCards_subsequentPage_hasPrevPage</td>
 *     <td>NOT CA-FIRST-PAGE → PF7 available</td>
 *   </tr>
 *   <tr>
 *     <td>listCards_noRecords_returnsEmptyContent</td>
 *     <td>WS-NO-RECORDS-FOUND condition set; "NO RECORDS TO SHOW" message</td>
 *   </tr>
 *   <tr>
 *     <td>getCard_existingCard_returns200</td>
 *     <td>S-select row → XCTL to COCRDSLC with CDEMO-CARD-NUM</td>
 *   </tr>
 *   <tr>
 *     <td>getCard_notFound_returns404</td>
 *     <td>READNEXT RESP = ENDFILE with no matching record</td>
 *   </tr>
 *   <tr>
 *     <td>listCards_defaultPageSize_is7</td>
 *     <td>WS-MAX-SCREEN-LINES PIC S9(4) COMP VALUE 7</td>
 *   </tr>
 *   <tr>
 *     <td>listCards_paginationMetadata_presentInResponse</td>
 *     <td>WS-CA-SCREEN-NUM / pageNumber in commarea</td>
 *   </tr>
 * </table>
 */
@WebMvcTest(CardListController.class)
class CardListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CardListService cardListService;

    @Autowired
    private ObjectMapper objectMapper;

    // -----------------------------------------------------------------------
    // Fixtures
    // -----------------------------------------------------------------------

    private CardData card1;
    private CardData card2;
    private CardData card3;

    @BeforeEach
    void setUp() {
        card1 = new CardData("4111111111111111", 12345678901L, "Y");
        card2 = new CardData("4222222222222222", 12345678901L, "Y");
        card3 = new CardData("4333333333333333", 99999999999L, "N");
    }

    // -----------------------------------------------------------------------
    // GET /api/cards – account-filtered listing (regular user)
    // -----------------------------------------------------------------------

    /**
     * COBOL: 9500-FILTER-RECORDS + 9000-READ-FORWARD with FLG-ACCTFILTER-ISVALID.
     * Only cards matching the given account are returned.
     */
    @Test
    @WithMockUser
    @DisplayName("GET /api/cards?accountId=X → returns only cards for that account")
    void listCards_withAccountId_returnsFilteredPage() throws Exception {
        List<CardData> cards = Arrays.asList(card1, card2);
        Page<CardData> page  = new PageImpl<>(cards, PageRequest.of(0, 7), 2);

        when(cardListService.listCards(eq(12345678901L), isNull(), eq(0), eq(7)))
                .thenReturn(page);

        mockMvc.perform(get("/api/cards")
                        .param("accountId", "12345678901")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].cardNum", is("4111111111111111")))
                .andExpect(jsonPath("$.content[0].accountId", is(12345678901L), Long.class))
                .andExpect(jsonPath("$.content[1].cardNum", is("4222222222222222")));

        verify(cardListService).listCards(eq(12345678901L), isNull(), eq(0), eq(7));
    }

    /**
     * COBOL: Admin path – no account filter, all cards returned.
     * Comment in COCRDLIC: "All cards if no context passed and admin user".
     */
    @Test
    @WithMockUser
    @DisplayName("GET /api/cards (no accountId) → admin view returns all cards")
    void listCards_adminNoAccountId_returnsAllCards() throws Exception {
        List<CardData> cards = Arrays.asList(card1, card2, card3);
        Page<CardData> page  = new PageImpl<>(cards, PageRequest.of(0, 7), 3);

        when(cardListService.listCards(isNull(), isNull(), eq(0), eq(7)))
                .thenReturn(page);

        mockMvc.perform(get("/api/cards").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.totalElements", is(3)));
    }

    // -----------------------------------------------------------------------
    // GET /api/cards – cursor (GTEQ) paging
    // -----------------------------------------------------------------------

    /**
     * COBOL PF8 (page-down):
     * MOVE WS-CA-LAST-CARD-NUM TO WS-CARD-RID-CARDNUM
     * EXEC CICS STARTBR RIDFLD(WS-CARD-RID-CARDNUM) GTEQ
     */
    @Test
    @WithMockUser
    @DisplayName("GET /api/cards?cursor=X → GTEQ browse from cursor card number (PF8 page-down)")
    void listCards_withCursor_usesGteqBrowse() throws Exception {
        List<CardData> cards = Collections.singletonList(card3);
        Page<CardData> page  = new PageImpl<>(cards, PageRequest.of(0, 7), 1);

        when(cardListService.listCards(isNull(), eq("4333333333333333"), eq(0), eq(7)))
                .thenReturn(page);

        mockMvc.perform(get("/api/cards")
                        .param("cursor", "4333333333333333")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].cardNum", is("4333333333333333")));

        verify(cardListService).listCards(isNull(), eq("4333333333333333"), eq(0), eq(7));
    }

    // -----------------------------------------------------------------------
    // Pagination flags – maps to COBOL commarea flags
    // -----------------------------------------------------------------------

    /**
     * COBOL: CA-NEXT-PAGE-EXISTS – set when more records exist after current page.
     * hasNextPage must be true when the page is not the last one.
     */
    @Test
    @WithMockUser
    @DisplayName("hasNextPage=true when more pages exist (CA-NEXT-PAGE-EXISTS)")
    void listCards_pageDownHasNextPage_flagsNextPageExists() throws Exception {
        List<CardData> cards = Arrays.asList(card1, card2);
        // totalElements=10, page 0 of size 7 → has next
        Page<CardData> page = new PageImpl<>(cards, PageRequest.of(0, 7), 10);

        when(cardListService.listCards(isNull(), isNull(), eq(0), eq(7))).thenReturn(page);

        mockMvc.perform(get("/api/cards").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasNextPage", is(true)))
                .andExpect(jsonPath("$.hasPrevPage", is(false)));
    }

    /**
     * COBOL: CA-FIRST-PAGE – when on page 0, PF7 (page-up) is disabled.
     * hasPrevPage must be false on page 0.
     */
    @Test
    @WithMockUser
    @DisplayName("hasPrevPage=false on first page (CA-FIRST-PAGE)")
    void listCards_firstPage_noPrevPage() throws Exception {
        Page<CardData> page = new PageImpl<>(
                List.of(card1), PageRequest.of(0, 7), 1);

        when(cardListService.listCards(isNull(), isNull(), eq(0), eq(7))).thenReturn(page);

        mockMvc.perform(get("/api/cards").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasPrevPage", is(false)));
    }

    /**
     * COBOL: NOT CA-FIRST-PAGE – on page > 0 PF7 (page-up) is available.
     * hasPrevPage must be true on pages > 0.
     */
    @Test
    @WithMockUser
    @DisplayName("hasPrevPage=true on subsequent pages (NOT CA-FIRST-PAGE)")
    void listCards_subsequentPage_hasPrevPage() throws Exception {
        Page<CardData> page = new PageImpl<>(
                List.of(card2), PageRequest.of(1, 7), 10);

        when(cardListService.listCards(isNull(), isNull(), eq(1), eq(7))).thenReturn(page);

        mockMvc.perform(get("/api/cards")
                        .param("page", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasPrevPage", is(true)))
                .andExpect(jsonPath("$.pageNumber", is(1)));
    }

    /**
     * COBOL: WS-NO-RECORDS-FOUND condition when READNEXT returns ENDFILE on first read.
     * Empty content list is returned with totalElements = 0.
     */
    @Test
    @WithMockUser
    @DisplayName("No records found → empty content list (WS-NO-RECORDS-FOUND)")
    void listCards_noRecords_returnsEmptyContent() throws Exception {
        Page<CardData> emptyPage = new PageImpl<>(
                Collections.emptyList(), PageRequest.of(0, 7), 0);

        when(cardListService.listCards(eq(99999999900L), isNull(), eq(0), eq(7)))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/api/cards")
                        .param("accountId", "99999999900")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    // -----------------------------------------------------------------------
    // GET /api/cards/{cardNum}
    // -----------------------------------------------------------------------

    /**
     * COBOL: S-select row → XCTL PROGRAM(COCRDSLC) with CDEMO-CARD-NUM.
     * Returns the full card record for client-side navigation to the detail screen.
     */
    @Test
    @WithMockUser
    @DisplayName("GET /api/cards/{cardNum} existing card → 200 + card payload")
    void getCard_existingCard_returns200() throws Exception {
        when(cardListService.findByCardNum("4111111111111111"))
                .thenReturn(Optional.of(card1));

        mockMvc.perform(get("/api/cards/4111111111111111")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardNum",    is("4111111111111111")))
                .andExpect(jsonPath("$.accountId",  is(12345678901L), Long.class))
                .andExpect(jsonPath("$.activeStatus", is("Y")));
    }

    /**
     * COBOL: READNEXT → ENDFILE with no matching record for the supplied card number.
     */
    @Test
    @WithMockUser
    @DisplayName("GET /api/cards/{cardNum} not found → 404")
    void getCard_notFound_returns404() throws Exception {
        when(cardListService.findByCardNum("0000000000000000"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/cards/0000000000000000")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------------
    // Default page size constant
    // -----------------------------------------------------------------------

    /**
     * COBOL: WS-MAX-SCREEN-LINES PIC S9(4) COMP VALUE 7.
     * When no 'size' param is supplied the default must be 7.
     */
    @Test
    @WithMockUser
    @DisplayName("Default page size is 7 matching WS-MAX-SCREEN-LINES")
    void listCards_defaultPageSize_is7() throws Exception {
        Page<CardData> page = new PageImpl<>(
                List.of(card1), PageRequest.of(0, 7), 1);

        when(cardListService.listCards(isNull(), isNull(), eq(0), eq(7))).thenReturn(page);

        mockMvc.perform(get("/api/cards").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageSize", is(7)));

        // Verify service was called with size=7
        verify(cardListService).listCards(isNull(), isNull(), eq(0), eq(7));
    }

    // -----------------------------------------------------------------------
    // Pagination metadata completeness
    // -----------------------------------------------------------------------

    /**
     * Response envelope must contain all pagination metadata that maps to COBOL
     * commarea / screen counter fields.
     */
    @Test
    @WithMockUser
    @DisplayName("Response contains full pagination metadata (pageNumber, totalPages, etc.)")
    void listCards_paginationMetadata_presentInResponse() throws Exception {
        List<CardData> cards = Arrays.asList(card1, card2);
        Page<CardData> page  = new PageImpl<>(cards, PageRequest.of(0, 7), 14);

        when(cardListService.listCards(isNull(), isNull(), eq(0), eq(7))).thenReturn(page);

        mockMvc.perform(get("/api/cards").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageNumber",    is(0)))
                .andExpect(jsonPath("$.pageSize",      is(7)))
                .andExpect(jsonPath("$.totalElements", is(14)))
                .andExpect(jsonPath("$.totalPages",    is(2)))
                .andExpect(jsonPath("$.hasNextPage",   is(true)))
                .andExpect(jsonPath("$.hasPrevPage",   is(false)));
    }

    // -----------------------------------------------------------------------
    // Combined cursor + accountId
    // -----------------------------------------------------------------------

    /**
     * When both accountId and cursor are supplied the service should receive both.
     * COBOL: FLG-ACCTFILTER-ISVALID + STARTBR from WS-CA-LAST-CARD-NUM GTEQ.
     */
    @Test
    @WithMockUser
    @DisplayName("GET /api/cards?accountId=X&cursor=Y → passes both to service")
    void listCards_accountIdAndCursor_bothPassedToService() throws Exception {
        Page<CardData> page = new PageImpl<>(
                List.of(card2), PageRequest.of(0, 7), 1);

        when(cardListService.listCards(eq(12345678901L), eq("4222222222222222"), eq(0), eq(7)))
                .thenReturn(page);

        mockMvc.perform(get("/api/cards")
                        .param("accountId", "12345678901")
                        .param("cursor",    "4222222222222222")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].cardNum", is("4222222222222222")));

        verify(cardListService)
                .listCards(eq(12345678901L), eq("4222222222222222"), eq(0), eq(7));
    }
}
