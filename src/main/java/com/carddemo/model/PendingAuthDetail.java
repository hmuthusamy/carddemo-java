package com.carddemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Java model for COBOL copybook CIPAUDTY.cpy
 * IMS Segment - Pending Authorization Details.
 * VSAM / IMS KSDS equivalent - keyed by composite authorization key.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pending_auth_detail")
public class PendingAuthDetail {

    /** PA-AUTH-DATE-9C PIC S9(05) COMP-3 - part of composite key */
    @Id
    @Column(name = "pa_auth_date_9c", precision = 5, nullable = false)
    private BigDecimal paAuthDate9c;

    /** PA-AUTH-TIME-9C PIC S9(09) COMP-3 - part of composite key */
    @Column(name = "pa_auth_time_9c", precision = 9, nullable = false)
    private BigDecimal paAuthTime9c;

    /** PA-AUTH-ORIG-DATE PIC X(06) */
    @Column(name = "pa_auth_orig_date", length = 6)
    private String paAuthOrigDate;

    /** PA-AUTH-ORIG-TIME PIC X(06) */
    @Column(name = "pa_auth_orig_time", length = 6)
    private String paAuthOrigTime;

    /** PA-CARD-NUM PIC X(16) */
    @Column(name = "pa_card_num", length = 16)
    private String paCardNum;

    /** PA-AUTH-TYPE PIC X(04) */
    @Column(name = "pa_auth_type", length = 4)
    private String paAuthType;

    /** PA-CARD-EXPIRY-DATE PIC X(04) */
    @Column(name = "pa_card_expiry_date", length = 4)
    private String paCardExpiryDate;

    /** PA-MESSAGE-TYPE PIC X(06) */
    @Column(name = "pa_message_type", length = 6)
    private String paMessageType;

    /** PA-MESSAGE-SOURCE PIC X(06) */
    @Column(name = "pa_message_source", length = 6)
    private String paMessageSource;

    /** PA-AUTH-ID-CODE PIC X(06) */
    @Column(name = "pa_auth_id_code", length = 6)
    private String paAuthIdCode;

    /** PA-AUTH-RESP-CODE PIC X(02) - '00' = approved */
    @Column(name = "pa_auth_resp_code", length = 2)
    private String paAuthRespCode;

    /** PA-AUTH-RESP-REASON PIC X(04) */
    @Column(name = "pa_auth_resp_reason", length = 4)
    private String paAuthRespReason;

    /** PA-PROCESSING-CODE PIC 9(06) */
    @Column(name = "pa_processing_code", precision = 6)
    private Integer paProcessingCode;

    /** PA-TRANSACTION-AMT PIC S9(10)V99 COMP-3 */
    @Column(name = "pa_transaction_amt", precision = 12, scale = 2)
    private BigDecimal paTransactionAmt;

    /** PA-APPROVED-AMT PIC S9(10)V99 COMP-3 */
    @Column(name = "pa_approved_amt", precision = 12, scale = 2)
    private BigDecimal paApprovedAmt;

    /** PA-MERCHANT-CATAGORY-CODE PIC X(04) */
    @Column(name = "pa_merchant_category_code", length = 4)
    private String paMerchantCategoryCode;

    /** PA-ACQR-COUNTRY-CODE PIC X(03) */
    @Column(name = "pa_acqr_country_code", length = 3)
    private String paAcqrCountryCode;

    /** PA-POS-ENTRY-MODE PIC 9(02) */
    @Column(name = "pa_pos_entry_mode", precision = 2)
    private Integer paPosEntryMode;

    /** PA-MERCHANT-ID PIC X(15) */
    @Column(name = "pa_merchant_id", length = 15)
    private String paMerchantId;

    /** PA-MERCHANT-NAME PIC X(22) */
    @Column(name = "pa_merchant_name", length = 22)
    private String paMerchantName;

    /** PA-MERCHANT-CITY PIC X(13) */
    @Column(name = "pa_merchant_city", length = 13)
    private String paMerchantCity;

    /** PA-MERCHANT-STATE PIC X(02) */
    @Column(name = "pa_merchant_state", length = 2)
    private String paMerchantState;

    /** PA-MERCHANT-ZIP PIC X(09) */
    @Column(name = "pa_merchant_zip", length = 9)
    private String paMerchantZip;

    /** PA-TRANSACTION-ID PIC X(15) */
    @Column(name = "pa_transaction_id", length = 15)
    private String paTransactionId;

    /** PA-MATCH-STATUS PIC X(01) - P=Pending, D=Declined, E=Expired, M=Matched */
    @Column(name = "pa_match_status", length = 1)
    private String paMatchStatus;

    /** PA-AUTH-FRAUD PIC X(01) - F=Confirmed fraud, R=Removed */
    @Column(name = "pa_auth_fraud", length = 1)
    private String paAuthFraud;

    /** PA-FRAUD-RPT-DATE PIC X(08) */
    @Column(name = "pa_fraud_rpt_date", length = 8)
    private String paFraudRptDate;
}
