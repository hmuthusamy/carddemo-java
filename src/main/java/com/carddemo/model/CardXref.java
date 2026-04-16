package com.carddemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a card-to-account cross-reference record.
 *
 * <p>Migrated from COBOL copybook CVACT03Y (XREF-FILE in CBTRN01C).
 * The XREF file maps a 16-digit card number to an account ID and customer ID,
 * enabling the transaction processor to resolve which account a card charge
 * should be posted against.
 *
 * <p>Layout in COBOL:
 * <pre>
 *   FD-XREF-CARD-NUM   PIC X(16)   — record key
 *   FD-XREF-DATA       PIC X(34)   — XREF-ACCT-ID (11) + XREF-CUST-ID (9) + filler
 * </pre>
 */
@Entity
@Table(name = "card_xref")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardXref {

    /** 16-character card number (FD-XREF-CARD-NUM / XREF-CARD-NUM). */
    @Id
    @Column(name = "card_number", length = 16, nullable = false)
    private String cardNumber;

    /** Linked account ID (XREF-ACCT-ID). */
    @Column(name = "account_id", nullable = false)
    private Long accountId;

    /** Linked customer ID (XREF-CUST-ID). */
    @Column(name = "customer_id")
    private Long customerId;
}
