package com.carddemo.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * REST request body for PUT /api/cards/{cardNumber}.
 *
 * Maps to the COBOL CCUP-NEW-DETAILS commarea section:
 *   CCUP-NEW-ACCTID    PIC X(11)  -> accountId
 *   CCUP-NEW-CARDID    PIC X(16)  -> cardNumber  (path param, echoed here for validation)
 *   CCUP-NEW-CVV-CD    PIC X(03)  -> cvvCode
 *   CCUP-NEW-CRDNAME   PIC X(50)  -> embossedName
 *   CCUP-NEW-EXPYEAR   PIC X(04)  -> expiryYear
 *   CCUP-NEW-EXPMON    PIC X(02)  -> expiryMonth
 *   CCUP-NEW-EXPDAY    PIC X(02)  -> expiryDay
 *   CCUP-NEW-CRDSTCD   PIC X(01)  -> activeStatus  ('Y'/'N')
 *
 * Validations mirror paragraphs 1210-EDIT-ACCOUNT through 1260-EDIT-EXPIRY-YEAR.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardUpdateRequest {

    /**
     * CCUP-NEW-ACCTID – 11-digit numeric account id.
     * Paragraph 1210-EDIT-ACCOUNT: must be numeric, non-zero, 11 digits.
     */
    @NotBlank(message = "Account number not provided")
    @Pattern(regexp = "\\d{11}", message = "Account number must be a non zero 11 digit number")
    private String accountId;

    /**
     * CCUP-NEW-CRDNAME – card embossed name, alphabets and spaces only.
     * Paragraph 1230-EDIT-NAME: not blank, only A-Z a-z and spaces.
     */
    @NotBlank(message = "Card name not provided")
    @Size(max = 50, message = "Card name must not exceed 50 characters")
    @Pattern(regexp = "[A-Za-z ]+", message = "Card name can only contain alphabets and spaces")
    private String embossedName;

    /**
     * CCUP-NEW-EXPYEAR – 4-digit expiry year.
     * Paragraph 1260-EDIT-EXPIRY-YEAR: VALID-YEAR 1950 THRU 2099.
     */
    @NotBlank(message = "Invalid card expiry year")
    @Pattern(regexp = "(19[5-9]\\d|20[0-9]\\d)", message = "Invalid card expiry year")
    private String expiryYear;

    /**
     * CCUP-NEW-EXPMON – 2-digit expiry month (01–12).
     * Paragraph 1250-EDIT-EXPIRY-MON: VALID-MONTH 1 THRU 12.
     */
    @NotBlank(message = "Card expiry month must be between 1 and 12")
    @Pattern(regexp = "0[1-9]|1[0-2]", message = "Card expiry month must be between 1 and 12")
    private String expiryMonth;

    /**
     * CCUP-NEW-EXPDAY – 2-digit expiry day (01–31).
     * Kept for completeness; COBOL notes it was non-display (not user-editable).
     */
    @Pattern(regexp = "0[1-9]|[12]\\d|3[01]", message = "Card expiry day must be between 01 and 31")
    private String expiryDay;

    /**
     * CCUP-NEW-CRDSTCD – active status flag.
     * Paragraph 1240-EDIT-CARDSTATUS: must be 'Y' or 'N'.
     */
    @NotBlank(message = "Card Active Status must be Y or N")
    @Pattern(regexp = "[YN]", message = "Card Active Status must be Y or N")
    private String activeStatus;

    /**
     * CCUP-NEW-CVV-CD – 3-digit CVV code, numeric.
     * From CARD-CVV-CD-X PIC X(03) / CARD-CVV-CD-N PIC 9(03).
     */
    @Pattern(regexp = "\\d{3}", message = "CVV must be a 3-digit number")
    private String cvvCode;
}
