package com.carddemo.controller;

import com.carddemo.model.TransactionData;
import com.carddemo.service.TransactionListService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit / slice tests for {@link TransactionListController}.
 *
 * <p>Uses {@code @WebMvcTest} to load only the web layer (no Spring Security,
 * no JPA context), with the service layer mocked via {@code @MockBean}.
 *
 * <h3>Scenarios tested</h3>
 * <ol>
 *   <li>Happy-path list – page 0, default size 10 → HTTP 200 + JSON body</li>
 *   <li>PF8 navigation (page forward) – page=1 forwarded to service</li>
 *   <li>PF7 navigation (page backward) – page=0 (floor) forwarded to service</li>
 *   <li>Date-range filter – startDate &amp; endDate parsed and forwarded</li>
 *   <li>Missing / blank accountId → HTTP 400</li>
 *   <li>Empty result set → HTTP 200, empty content array</li>
 *   <li>Custom page size – size=5 forwarded to service</li>
 * </ol>
 */
@WebMvcTest(controllers = TransactionListController.class)
@AutoConfigureMockMvc(addFilters = false)   // disable Security filter chain for unit test
class TransactionListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionListService transactionListService;

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private TransactionData buildTransaction(String id) {
        return TransactionData.builder()
                .tranId(id)
                .tranCardNum("4111111111111111")
                .tranTypeCd("PU")
                .tranCatCd(1)
                .tranSource("WEB")
                .tranDesc("Test transaction " + id)
                .tranAmt(new BigDecimal("123.45"))
                .tranMerchantId(123456789L)
                .tranMerchantName("ACME Store")
                .tranMerchantCity("Seattle")
                .tranMerchantZip("98101")
                .tranOrigTs(LocalDateTime.of(2023, 6, 15, 10, 30, 0))
                .tranProcTs(LocalDateTime.of(2023, 6, 15, 10, 31, 5))
                .build();
    }

    private Page<TransactionData> singlePage(String... ids) {
        List<TransactionData> items = List.of(ids).stream()
                .map(this::buildTransaction)
                .toList();
        return new PageImpl<>(items, PageRequest.of(0, 10), items.size());
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/transactions?accountId=xxx returns HTTP 200 with paged JSON")
    void happyPath_returnsPagedJson() throws Exception {
        Page<TransactionData> fakePage = singlePage("TXN001", "TXN002", "TXN003");
        when(transactionListService.listTransactions(
                eq("4111111111111111"), eq(0), eq(10), isNull(), isNull()))
                .thenReturn(fakePage);

        mockMvc.perform(get("/api/transactions")
                        .param("accountId", "4111111111111111")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.content[0].tranId").value("TXN001"))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.number").value(0));

        verify(transactionListService).listTransactions(
                "4111111111111111", 0, 10, null, null);
    }

    @Test
    @DisplayName("PF8 forward navigation – page=1 is forwarded to service")
    void pageForward_pf8_passesPageOneToService() throws Exception {
        Page<TransactionData> fakePage = singlePage("TXN011");
        when(transactionListService.listTransactions(
                eq("4111111111111111"), eq(1), eq(10), isNull(), isNull()))
                .thenReturn(fakePage);

        mockMvc.perform(get("/api/transactions")
                        .param("accountId", "4111111111111111")
                        .param("page", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tranId").value("TXN011"));

        ArgumentCaptor<Integer> pageCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(transactionListService).listTransactions(
                anyString(), pageCaptor.capture(), anyInt(), isNull(), isNull());
        assertThat(pageCaptor.getValue()).isEqualTo(1);
    }

    @Test
    @DisplayName("PF7 backward navigation – page=0 (floor) forwarded to service")
    void pageBackward_pf7_floorAtZero() throws Exception {
        Page<TransactionData> fakePage = singlePage("TXN001");
        when(transactionListService.listTransactions(
                eq("4111111111111111"), eq(0), eq(10), isNull(), isNull()))
                .thenReturn(fakePage);

        mockMvc.perform(get("/api/transactions")
                        .param("accountId", "4111111111111111")
                        .param("page", "0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        ArgumentCaptor<Integer> pageCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(transactionListService).listTransactions(
                anyString(), pageCaptor.capture(), anyInt(), isNull(), isNull());
        assertThat(pageCaptor.getValue()).isZero();
    }

    @Test
    @DisplayName("Date-range filter – startDate and endDate are parsed and forwarded")
    void dateRangeFilter_parsedAndForwardedToService() throws Exception {
        LocalDate start = LocalDate.of(2023, 1, 1);
        LocalDate end   = LocalDate.of(2023, 12, 31);
        Page<TransactionData> fakePage = singlePage("TXN100");
        when(transactionListService.listTransactions(
                eq("4111111111111111"), eq(0), eq(10), eq(start), eq(end)))
                .thenReturn(fakePage);

        mockMvc.perform(get("/api/transactions")
                        .param("accountId", "4111111111111111")
                        .param("startDate", "2023-01-01")
                        .param("endDate",   "2023-12-31")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tranId").value("TXN100"));

        verify(transactionListService).listTransactions(
                "4111111111111111", 0, 10, start, end);
    }

    @Test
    @DisplayName("Missing accountId → HTTP 400")
    void missingAccountId_returns400() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionListService);
    }

    @Test
    @DisplayName("Blank accountId → HTTP 400")
    void blankAccountId_returns400() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .param("accountId", "   ")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionListService);
    }

    @Test
    @DisplayName("Empty result set → HTTP 200, empty content array")
    void emptyResult_returns200WithEmptyContent() throws Exception {
        Page<TransactionData> emptyPage =
                new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(transactionListService.listTransactions(
                eq("9999999999999999"), eq(0), eq(10), isNull(), isNull()))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/api/transactions")
                        .param("accountId", "9999999999999999")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("Custom page size – size=5 is forwarded to service")
    void customPageSize_forwardedToService() throws Exception {
        Page<TransactionData> fakePage = singlePage("TXN001", "TXN002");
        when(transactionListService.listTransactions(
                eq("4111111111111111"), eq(0), eq(5), isNull(), isNull()))
                .thenReturn(fakePage);

        mockMvc.perform(get("/api/transactions")
                        .param("accountId", "4111111111111111")
                        .param("size", "5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        ArgumentCaptor<Integer> sizeCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(transactionListService).listTransactions(
                anyString(), anyInt(), sizeCaptor.capture(), isNull(), isNull());
        assertThat(sizeCaptor.getValue()).isEqualTo(5);
    }

    @Test
    @DisplayName("Response includes pagination metadata matching COBOL page counters")
    void responsePaginationMetadata_matchesCobolPageCounters() throws Exception {
        // Simulate 25 total records, page 1 of 3
        List<TransactionData> items =
                List.of("T01","T02","T03","T04","T05","T06","T07","T08","T09","T10")
                        .stream().map(this::buildTransaction).toList();
        Page<TransactionData> page1 = new PageImpl<>(items, PageRequest.of(1, 10), 25);

        when(transactionListService.listTransactions(
                eq("4111111111111111"), eq(1), eq(10), isNull(), isNull()))
                .thenReturn(page1);

        mockMvc.perform(get("/api/transactions")
                        .param("accountId", "4111111111111111")
                        .param("page", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // COBOL: CDEMO-CT00-PAGE-NUM = 2 (1-based) → Spring page.number = 1 (0-based)
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.totalElements").value(25))
                // CDEMO-CT00-NEXT-PAGE-FLG = 'Y' (has next page) → hasNext = true
                .andExpect(jsonPath("$.last").value(false))
                .andExpect(jsonPath("$.first").value(false));
    }
}
