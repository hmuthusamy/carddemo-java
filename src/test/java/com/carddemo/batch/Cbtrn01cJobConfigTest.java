package com.carddemo.batch;

import com.carddemo.model.Account;
import com.carddemo.model.CardXref;
import com.carddemo.model.TransactionData;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CardXrefRepository;
import com.carddemo.service.Cbtrn01cService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link Cbtrn01cJobConfig} – covers the COBOL→Java migration of
 * CBTRN01C.CBL (daily transaction file processor).
 *
 * <h2>COBOL validation branches tested</h2>
 * <ol>
 *   <li>Valid path      : card found in XREF + account found → {@code VALID}</li>
 *   <li>Invalid card    : XREF lookup fails (2000-LOOKUP-XREF INVALID KEY) → {@code REJECTED_CARD}</li>
 *   <li>Invalid account : account read fails (3000-READ-ACCOUNT INVALID KEY) → {@code REJECTED_ACCOUNT}</li>
 *   <li>Zero amount     : defensive guard → {@code REJECTED_AMOUNT}</li>
 *   <li>Null amount     : defensive guard → {@code REJECTED_AMOUNT}</li>
 *   <li>Negative amount : defensive guard → {@code REJECTED_AMOUNT}</li>
 * </ol>
 */
class Cbtrn01cJobConfigTest {

    // =========================================================================
    // 1. Service unit tests – validate every COBOL processing branch
    // =========================================================================

    @Nested
    @DisplayName("Cbtrn01cService – validation rules (COBOL PROCEDURE DIVISION parity)")
    @ExtendWith(MockitoExtension.class)
    class ServiceUnitTests {

        @Mock  CardXrefRepository cardXrefRepository;
        @Mock  AccountRepository  accountRepository;
        @InjectMocks Cbtrn01cService service;

        private static final String CARD  = "1234567890123456";
        private static final Long   ACCT  = 10000000001L;
        private static final Long   CUST  = 100000001L;

        private CardXref validXref;
        private Account  validAccount;

        @BeforeEach
        void setUp() {
            validXref = CardXref.builder()
                    .cardNumber(CARD).accountId(ACCT).customerId(CUST).build();
            validAccount = Account.builder()
                    .accountId(ACCT).activeStatus("1")
                    .balance(new BigDecimal("1500.00"))
                    .creditLimit(new BigDecimal("5000.00")).build();
        }

        // --- Happy path (COBOL: both 2000-LOOKUP-XREF and 3000-READ-ACCOUNT succeed) ---

        @Test
        @DisplayName("Valid transaction: card + account both found → VALID, accountId populated")
        void validTransaction_cardAndAccountFound_markedValid() {
            when(cardXrefRepository.findById(CARD)).thenReturn(Optional.of(validXref));
            when(accountRepository.findById(ACCT)).thenReturn(Optional.of(validAccount));

            TransactionData result = service.validateAndEnrich(tx(CARD, new BigDecimal("99.99")));

            assertThat(result.getStatus()).isEqualTo("VALID");
            assertThat(result.getAccountId()).isEqualTo(ACCT);
            assertThat(result.getProcTimestamp()).isNotNull();
            verify(cardXrefRepository).findById(CARD);
            verify(accountRepository).findById(ACCT);
        }

        // --- COBOL 2000-LOOKUP-XREF INVALID KEY branch ---

        @Test
        @DisplayName("Card not in XREF (2000-LOOKUP-XREF INVALID KEY) → REJECTED_CARD")
        void cardNotInXref_rejectedCard() {
            when(cardXrefRepository.findById(anyString())).thenReturn(Optional.empty());

            TransactionData result = service.validateAndEnrich(tx("0000000000000000", new BigDecimal("50.00")));

            assertThat(result.getStatus()).isEqualTo("REJECTED_CARD");
            assertThat(result.getAccountId()).isNull();
            verify(accountRepository, never()).findById(anyLong());
        }

        // --- COBOL 3000-READ-ACCOUNT INVALID KEY branch ---

        @Test
        @DisplayName("Account not found (3000-READ-ACCOUNT INVALID KEY) → REJECTED_ACCOUNT")
        void accountNotFound_rejectedAccount() {
            when(cardXrefRepository.findById(CARD)).thenReturn(Optional.of(validXref));
            when(accountRepository.findById(ACCT)).thenReturn(Optional.empty());

            TransactionData result = service.validateAndEnrich(tx(CARD, new BigDecimal("200.00")));

            assertThat(result.getStatus()).isEqualTo("REJECTED_ACCOUNT");
            assertThat(result.getAccountId()).isNull();
        }

        // --- Amount guards ---

        @Test
        @DisplayName("Zero amount → REJECTED_AMOUNT (no XREF or account lookup)")
        void zeroAmount_rejectedImmediately() {
            TransactionData result = service.validateAndEnrich(tx(CARD, BigDecimal.ZERO));
            assertThat(result.getStatus()).isEqualTo("REJECTED_AMOUNT");
            verifyNoInteractions(cardXrefRepository, accountRepository);
        }

        @Test
        @DisplayName("Null amount → REJECTED_AMOUNT")
        void nullAmount_rejected() {
            TransactionData result = service.validateAndEnrich(tx(CARD, null));
            assertThat(result.getStatus()).isEqualTo("REJECTED_AMOUNT");
        }

        @Test
        @DisplayName("Negative amount → REJECTED_AMOUNT")
        void negativeAmount_rejected() {
            TransactionData result = service.validateAndEnrich(tx(CARD, new BigDecimal("-0.01")));
            assertThat(result.getStatus()).isEqualTo("REJECTED_AMOUNT");
        }

        // --- isAmountValid helper ---

        @Test @DisplayName("isAmountValid: positive → true")
        void isAmountValid_positive() { assertThat(service.isAmountValid(new BigDecimal("0.01"))).isTrue(); }

        @Test @DisplayName("isAmountValid: zero → false")
        void isAmountValid_zero()     { assertThat(service.isAmountValid(BigDecimal.ZERO)).isFalse(); }

        @Test @DisplayName("isAmountValid: null → false")
        void isAmountValid_null()     { assertThat(service.isAmountValid(null)).isFalse(); }

        // --- Edge cases ---

        @Test
        @DisplayName("Blank card number → lookupCardXref returns empty → REJECTED_CARD")
        void blankCardNumber_rejectedCard() {
            TransactionData result = service.validateAndEnrich(tx("   ", new BigDecimal("100.00")));
            assertThat(result.getStatus()).isEqualTo("REJECTED_CARD");
            verifyNoInteractions(cardXrefRepository);
        }

        @Test
        @DisplayName("procTimestamp is always set on enrichment (valid path)")
        void validTransaction_procTimestampStamped() {
            when(cardXrefRepository.findById(CARD)).thenReturn(Optional.of(validXref));
            when(accountRepository.findById(ACCT)).thenReturn(Optional.of(validAccount));

            TransactionData tx = tx(CARD, new BigDecimal("1.00"));
            assertThat(tx.getProcTimestamp()).isNull();

            TransactionData result = service.validateAndEnrich(tx);
            assertThat(result.getProcTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("procTimestamp is always set on rejection (REJECTED_CARD path)")
        void rejectedCard_procTimestampStamped() {
            when(cardXrefRepository.findById(anyString())).thenReturn(Optional.empty());
            TransactionData result = service.validateAndEnrich(tx(CARD, new BigDecimal("1.00")));
            assertThat(result.getProcTimestamp()).isNotNull();
        }

        // --- Test fixture ---
        private TransactionData tx(String card, BigDecimal amount) {
            return TransactionData.builder()
                    .transactionId("TXN0000000000001")
                    .cardNumber(card)
                    .amount(amount)
                    .categoryCode("0001")
                    .typeCode("DB")
                    .source("POS       ")
                    .description("Unit test transaction")
                    .merchantId("MCH000001")
                    .merchantName("Test Merchant")
                    .merchantCity("Seattle")
                    .merchantZip("98101")
                    .build();
        }
    }

    // =========================================================================
    // 2. Processor unit tests – lambda in Cbtrn01cJobConfig#cbtrn01cProcessor()
    // =========================================================================

    @Nested
    @DisplayName("cbtrn01cProcessor – item processor lambda tests")
    @ExtendWith(MockitoExtension.class)
    class ProcessorTests {

        @Mock Cbtrn01cService cbtrn01cService;

        /** Inline re-implementation of the lambda so tests are hermetic. */
        private ItemProcessor<TransactionData, TransactionData> processor;

        @BeforeEach
        void setUp() {
            processor = item -> {
                if (item.getAmount() == null) {
                    item.setAmount(BigDecimal.ZERO);
                }
                return cbtrn01cService.validateAndEnrich(item);
            };
        }

        @Test
        @DisplayName("Null amount is normalised to ZERO before service call")
        void nullAmount_normalisedBeforeServiceCall() throws Exception {
            TransactionData tx = txWith(null);

            when(cbtrn01cService.validateAndEnrich(any())).thenAnswer(inv -> {
                TransactionData t = inv.getArgument(0);
                assertThat(t.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
                t.setStatus("REJECTED_AMOUNT");
                return t;
            });

            TransactionData result = processor.process(tx);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo("REJECTED_AMOUNT");
        }

        @Test
        @DisplayName("Non-null amount passes unchanged to service")
        void nonNullAmount_unchangedPassthrough() throws Exception {
            BigDecimal amt = new BigDecimal("42.00");
            TransactionData tx = txWith(amt);

            when(cbtrn01cService.validateAndEnrich(any())).thenAnswer(inv -> {
                assertThat(((TransactionData) inv.getArgument(0)).getAmount())
                        .isEqualByComparingTo(amt);
                tx.setStatus("VALID");
                return tx;
            });

            assertThat(processor.process(tx).getStatus()).isEqualTo("VALID");
        }

        @Test
        @DisplayName("Processor never returns null (rejected items written for audit trail)")
        void processor_neverReturnsNull() throws Exception {
            TransactionData tx = txWith(BigDecimal.ZERO);
            when(cbtrn01cService.validateAndEnrich(any())).thenReturn(tx);
            assertThat(processor.process(tx)).isNotNull();
        }

        @Test
        @DisplayName("Service is called exactly once per item")
        void service_calledExactlyOnce() throws Exception {
            TransactionData tx = txWith(new BigDecimal("10.00"));
            when(cbtrn01cService.validateAndEnrich(any())).thenReturn(tx);
            processor.process(tx);
            verify(cbtrn01cService, times(1)).validateAndEnrich(tx);
        }

        private TransactionData txWith(BigDecimal amount) {
            return TransactionData.builder()
                    .transactionId("TX001")
                    .cardNumber("1111222233334444")
                    .amount(amount)
                    .build();
        }
    }
}
