package com.carddemo.model;

import java.math.BigDecimal;

/**
 * Maps to FD-TRNXFILE-REC from CBSTM03B.CBL / COSTM01 copybook.
 *
 * Layout (350 bytes total):
 *   FD-TRNXS-ID  (32 bytes)
 *     FD-TRNX-CARD  PIC X(16)
 *     FD-TRNX-ID    PIC X(16)
 *   FD-ACCT-DATA  PIC X(318)
 *     -> TRNX-DESC   PIC X(50) at offset 0 of FD-ACCT-DATA
 *     -> TRNX-AMT    PIC S9(9)V99 COMP-3 (represented as BigDecimal)
 *     -> remaining fields
 */
public class TransactionData {

    /** Card number – TRNX-CARD-NUM / FD-TRNX-CARD PIC X(16) */
    private String cardNumber;

    /** Transaction ID – TRNX-ID / FD-TRNX-ID PIC X(16) */
    private String transactionId;

    /** Transaction description – TRNX-DESC PIC X(50) */
    private String transactionDesc;

    /** Transaction amount – TRNX-AMT PIC S9(9)V99, scale 2 */
    private BigDecimal transactionAmount;

    /** Transaction type code */
    private String transactionType;

    /** Original transaction description remainder */
    private String transactionRest;

    public TransactionData() {}

    public TransactionData(String cardNumber, String transactionId,
                           String transactionDesc, BigDecimal transactionAmount) {
        this.cardNumber = cardNumber;
        this.transactionId = transactionId;
        this.transactionDesc = transactionDesc;
        setTransactionAmount(transactionAmount);
    }

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getTransactionDesc() { return transactionDesc; }
    public void setTransactionDesc(String transactionDesc) { this.transactionDesc = transactionDesc; }

    public BigDecimal getTransactionAmount() { return transactionAmount; }
    public void setTransactionAmount(BigDecimal transactionAmount) {
        this.transactionAmount = (transactionAmount != null)
            ? transactionAmount.setScale(2, java.math.RoundingMode.HALF_UP)
            : null;
    }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public String getTransactionRest() { return transactionRest; }
    public void setTransactionRest(String transactionRest) { this.transactionRest = transactionRest; }

    @Override
    public String toString() {
        return "TransactionData{cardNumber='" + cardNumber + "', transactionId='" + transactionId
            + "', desc='" + transactionDesc + "', amount=" + transactionAmount + '}';
    }
}
