package com.carddemo.service;

import com.carddemo.model.CardXref;
import com.carddemo.model.TransactionAddRequest;
import com.carddemo.model.TransactionAddResponse;
import com.carddemo.model.TransactionData;
import com.carddemo.repository.CardXrefRepository;
import com.carddemo.repository.TransactionRepository;
import com.carddemo.service.TransactionAddService.TransactionValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TransactionAddServiceTest – unit tests for the service layer.
 *
 * <p>Exercises the individual COBOL-paragraph equivalents:
 * <ul>
 *   <li>VALIDATE-INPUT-KEY-FIELDS → resolveAndValidateKeys()</li>
 *   <li>VALIDATE-INPUT-DATA-FIELDS → validateDataFields()</li>
 *   <li>ADD-TRANSACTION / STARTBR+READPREV+ADD 1 → generateNextTransactionId()</li>
 *   <li>EXEC CICS WRITE → transactionRepository.save()</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("COTRNADD – TransactionAddService tests")
class TransactionAddServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CardXrefRepository cardXrefRepository;

    @InjectMocks
    private TransactionAddService service;

    private CardXref sampleXref;
    private TransactionData savedTran;

    @BeforeEach
    void setUp() {
        sampleXref = CardXref.builder()
                .xrefCardNum("4111111111111111")
                .xrefAcctId("00000001001")
                .xrefCustId("000000001")
                .build();

        savedTran = TransactionData.builder()
                .tranId("0000000000000001")
                .tranTypeCd("01")
                .tranCatCd("0001")
                .tranSource("POS")
                .tranDesc("Purchase at test merchant")
                .tranAmt(new BigDecimal("-125.99"))
                .tranCardNum("4111111111111111")
                .tranMerchantId("000000001")
                .tranMerchantName("Test Merchant")
                .tranMerchantCity("Austin")
                .tranMerchantZip("78701")
                .tranOrigTs("2024-01-15")
                .tranProcTs("2024-01-16")
                .build();
    }

    // ------------------------------------------------------------------ helpers

    private TransactionAddRequest validRequest() {
        return TransactionAddRequest.builder()
                .accountId("00000001001")
                .typeCode("01")
                .categoryCode("0001")
                .source("POS")
                .description("Purchase at test merchant")
                .amount(new BigDecimal("-125.99"))
                .origDate("2024-01-15")
                .procDate("2024-01-16")
                .merchantId("000000001")
                .merchantName("Test Merchant")
                .merchantCity("Austin")
                .merchantZip("78701")
                .build();
    }

    // ================================================================== addTransaction – happy path

    @Test
    @DisplayName("addTransaction – account ID path → saves record and returns response with generated ID")
    void addTransaction_accountIdPath_savesAndReturns() {
        when(cardXrefRepository.findByXrefAcctId("00000001001")).thenReturn(Optional.of(sampleXref));
        when(transactionRepository.findTopByOrderByTranIdDesc()).thenReturn(Optional.empty());
        when(transactionRepository.save(any(TransactionData.class))).thenReturn(savedTran);

        TransactionAddResponse response = service.addTransaction(validRequest());

        assertThat(response.getTransactionId()).isEqualTo("0000000000000001");
        assertThat(response.getCardNumber()).isEqualTo("4111111111111111");
        assertThat(response.getAccountId()).isEqualTo("00000001001");
        assertThat(response.getMessage()).startsWith("Transaction added successfully.");
        assertThat(response.getMessage()).contains("0000000000000001");
    }

    @Test
    @DisplayName("addTransaction – card number path → resolves account ID via CCXREF and saves")
    void addTransaction_cardNumberPath_resolvesAccountAndSaves() {
        TransactionAddRequest req = validRequest();
        req.setAccountId(null);
        req.setCardNumber("4111111111111111");

        when(cardXrefRepository.findByXrefCardNum("4111111111111111")).thenReturn(Optional.of(sampleXref));
        when(transactionRepository.findTopByOrderByTranIdDesc()).thenReturn(Optional.empty());
        when(transactionRepository.save(any(TransactionData.class))).thenReturn(savedTran);

        TransactionAddResponse response = service.addTransaction(req);

        assertThat(response.getAccountId()).isEqualTo("00000001001");
        verify(cardXrefRepository).findByXrefCardNum("4111111111111111");
    }

    // ================================================================== generateNextTransactionId

    @Test
    @DisplayName("generateNextTransactionId – empty table → returns '0000000000000001' (COBOL: ENDFILE → MOVE ZEROS + ADD 1)")
    void generateNextTransactionId_emptyTable_returnsOne() {
        when(transactionRepository.findTopByOrderByTranIdDesc()).thenReturn(Optional.empty());

        String id = service.generateNextTransactionId();

        assertThat(id).isEqualTo("0000000000000001");
        assertThat(id).hasSize(16);
    }

    @Test
    @DisplayName("generateNextTransactionId – existing records → increments last TRAN-ID by 1 (COBOL: ADD 1 TO WS-TRAN-ID-N)")
    void generateNextTransactionId_existingRecords_incrementsByOne() {
        TransactionData last = TransactionData.builder()
                .tranId("0000000000000041")
                .build();
        when(transactionRepository.findTopByOrderByTranIdDesc()).thenReturn(Optional.of(last));

        String id = service.generateNextTransactionId();

        assertThat(id).isEqualTo("0000000000000042");
        assertThat(id).hasSize(16);
    }

    @Test
    @DisplayName("generateNextTransactionId – generated ID is always exactly 16 chars (PIC X(16))")
    void generateNextTransactionId_alwaysSixteenChars() {
        TransactionData last = TransactionData.builder()
                .tranId("0000000000000099")
                .build();
        when(transactionRepository.findTopByOrderByTranIdDesc()).thenReturn(Optional.of(last));

        String id = service.generateNextTransactionId();

        assertThat(id).hasSize(16);
        assertThat(id).isEqualTo("0000000000000100");
    }

    // ================================================================== resolveAndValidateKeys

    @Test
    @DisplayName("resolveAndValidateKeys – no account/card → throws (COBOL: 'Account or Card Number must be entered')")
    void resolveAndValidateKeys_noInput_throws() {
        TransactionAddRequest req = validRequest();
        req.setAccountId(null);
        req.setCardNumber(null);

        assertThatThrownBy(() -> service.resolveAndValidateKeys(req))
                .isInstanceOf(TransactionValidationException.class)
                .hasMessageContaining("Account or Card Number must be entered");
    }

    @Test
    @DisplayName("resolveAndValidateKeys – non-numeric account → throws (COBOL: 'Account ID must be Numeric')")
    void resolveAndValidateKeys_nonNumericAccount_throws() {
        TransactionAddRequest req = validRequest();
        req.setAccountId("ABCDE");

        assertThatThrownBy(() -> service.resolveAndValidateKeys(req))
                .isInstanceOf(TransactionValidationException.class)
                .hasMessageContaining("Account ID must be Numeric");
    }

    @Test
    @DisplayName("resolveAndValidateKeys – account not in XREF → throws (COBOL DFHRESP(NOTFND): 'Account ID NOT found')")
    void resolveAndValidateKeys_accountNotFound_throws() {
        when(cardXrefRepository.findByXrefAcctId(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveAndValidateKeys(validRequest()))
                .isInstanceOf(TransactionValidationException.class)
                .hasMessageContaining("Account ID NOT found");
    }

    @Test
    @DisplayName("resolveAndValidateKeys – non-numeric card number → throws (COBOL: 'Card Number must be Numeric')")
    void resolveAndValidateKeys_nonNumericCardNumber_throws() {
        TransactionAddRequest req = validRequest();
        req.setAccountId(null);
        req.setCardNumber("XXXX");

        assertThatThrownBy(() -> service.resolveAndValidateKeys(req))
                .isInstanceOf(TransactionValidationException.class)
                .hasMessageContaining("Card Number must be Numeric");
    }

    @Test
    @DisplayName("resolveAndValidateKeys – card not in XREF → throws (COBOL DFHRESP(NOTFND): 'Card Number NOT found')")
    void resolveAndValidateKeys_cardNotFound_throws() {
        TransactionAddRequest req = validRequest();
        req.setAccountId(null);
        req.setCardNumber("9999999999999999");

        when(cardXrefRepository.findByXrefCardNum(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveAndValidateKeys(req))
                .isInstanceOf(TransactionValidationException.class)
                .hasMessageContaining("Card Number NOT found");
    }

    // ================================================================== validateDataFields

    @Test
    @DisplayName("validateDataFields – empty type code → throws (COBOL: 'Type CD can NOT be empty')")
    void validateDataFields_emptyTypeCode_throws() {
        TransactionAddRequest req = validRequest();
        req.setTypeCode("");

        assertThatThrownBy(() -> service.validateDataFields(req))
                .isInstanceOf(TransactionValidationException.class)
                .hasMessageContaining("Type CD can NOT be empty");
    }

    @Test
    @DisplayName("validateDataFields – non-numeric type code → throws (COBOL: 'Type CD must be Numeric')")
    void validateDataFields_nonNumericTypeCode_throws() {
        TransactionAddRequest req = validRequest();
        req.setTypeCode("AB");

        assertThatThrownBy(() -> service.validateDataFields(req))
                .isInstanceOf(TransactionValidationException.class)
                .hasMessageContaining("Type CD must be Numeric");
    }

    @Test
    @DisplayName("validateDataFields – null amount → throws (COBOL: 'Amount can NOT be empty')")
    void validateDataFields_nullAmount_throws() {
        TransactionAddRequest req = validRequest();
        req.setAmount(null);

        assertThatThrownBy(() -> service.validateDataFields(req))
                .isInstanceOf(TransactionValidationException.class)
                .hasMessageContaining("Amount can NOT be empty");
    }

    @Test
    @DisplayName("validateDataFields – zero amount → throws (COBOL: amount must be non-zero)")
    void validateDataFields_zeroAmount_throws() {
        TransactionAddRequest req = validRequest();
        req.setAmount(BigDecimal.ZERO);

        assertThatThrownBy(() -> service.validateDataFields(req))
                .isInstanceOf(TransactionValidationException.class)
                .hasMessageContaining("Amount should be in format");
    }

    @Test
    @DisplayName("validateDataFields – invalid orig date format → throws (COBOL: 'Orig Date should be in format YYYY-MM-DD')")
    void validateDataFields_invalidOrigDate_throws() {
        TransactionAddRequest req = validRequest();
        req.setOrigDate("15-01-2024");

        assertThatThrownBy(() -> service.validateDataFields(req))
                .isInstanceOf(TransactionValidationException.class)
                .hasMessageContaining("Orig Date should be in format YYYY-MM-DD");
    }

    @Test
    @DisplayName("validateDataFields – invalid proc date format → throws (COBOL: 'Proc Date should be in format YYYY-MM-DD')")
    void validateDataFields_invalidProcDate_throws() {
        TransactionAddRequest req = validRequest();
        req.setProcDate("2024/01/16");

        assertThatThrownBy(() -> service.validateDataFields(req))
                .isInstanceOf(TransactionValidationException.class)
                .hasMessageContaining("Proc Date should be in format YYYY-MM-DD");
    }

    @Test
    @DisplayName("validateDataFields – non-numeric merchant ID → throws (COBOL: 'Merchant ID must be Numeric')")
    void validateDataFields_nonNumericMerchantId_throws() {
        TransactionAddRequest req = validRequest();
        req.setMerchantId("MERCH001");

        assertThatThrownBy(() -> service.validateDataFields(req))
                .isInstanceOf(TransactionValidationException.class)
                .hasMessageContaining("Merchant ID must be Numeric");
    }

    @Test
    @DisplayName("validateDataFields – empty description → throws (COBOL: 'Description can NOT be empty')")
    void validateDataFields_emptyDescription_throws() {
        TransactionAddRequest req = validRequest();
        req.setDescription("  ");

        assertThatThrownBy(() -> service.validateDataFields(req))
                .isInstanceOf(TransactionValidationException.class)
                .hasMessageContaining("Description can NOT be empty");
    }

    // ================================================================== persistence verification

    @Test
    @DisplayName("addTransaction – saved record fields match COBOL TRAN-RECORD layout")
    void addTransaction_savedRecordMatchesCobolLayout() {
        when(cardXrefRepository.findByXrefAcctId("00000001001")).thenReturn(Optional.of(sampleXref));
        when(transactionRepository.findTopByOrderByTranIdDesc()).thenReturn(Optional.empty());
        when(transactionRepository.save(any(TransactionData.class))).thenReturn(savedTran);

        service.addTransaction(validRequest());

        ArgumentCaptor<TransactionData> captor = ArgumentCaptor.forClass(TransactionData.class);
        verify(transactionRepository).save(captor.capture());

        TransactionData persisted = captor.getValue();
        // All TRAN-RECORD fields populated (mirrors COBOL ADD-TRANSACTION paragraph)
        assertThat(persisted.getTranId()).hasSize(16);
        assertThat(persisted.getTranTypeCd()).isEqualTo("01");
        assertThat(persisted.getTranCatCd()).isEqualTo("0001");
        assertThat(persisted.getTranSource()).isEqualTo("POS");
        assertThat(persisted.getTranAmt()).isEqualByComparingTo(new BigDecimal("-125.99"));
        assertThat(persisted.getTranCardNum()).isEqualTo("4111111111111111");
        assertThat(persisted.getTranMerchantId()).isEqualTo("000000001");
        assertThat(persisted.getTranOrigTs()).isEqualTo("2024-01-15");
        assertThat(persisted.getTranProcTs()).isEqualTo("2024-01-16");
    }
}
