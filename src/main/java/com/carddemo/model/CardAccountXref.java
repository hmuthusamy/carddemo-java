package com.carddemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JPA entity for the CARD_ACCOUNT_XREF table.
 *
 * <p>Migrated from COBOL copybook CVACT03Y.cpy (CARD-XREF-RECORD, RECLN=50).
 * This cross-reference file links card numbers to their owning customer
 * and account, preserving the VSAM KSDS primary key structure.
 *
 * <pre>
 * Column type mapping:
 *   card_number   VARCHAR(16)  ← XREF-CARD-NUM PIC X(16)  (VSAM primary key)
 *   customer_id   INTEGER      ← XREF-CUST-ID  PIC 9(09)
 *   account_id    BIGINT       ← XREF-ACCT-ID  PIC 9(11)
 * </pre>
 */
@Entity
@Table(name = "card_account_xref")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardAccountXref {

    /** XREF-CARD-NUM PIC X(16) – primary key. Maps to DDL: VARCHAR(16) NOT NULL */
    @Id
    @Column(name = "card_number", nullable = false, length = 16)
    private String cardNumber;

    /** XREF-CUST-ID PIC 9(09) – FK to customers. Maps to DDL: INTEGER NOT NULL */
    @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    /** XREF-ACCT-ID PIC 9(11) – FK to accounts. Maps to DDL: BIGINT NOT NULL */
    @Column(name = "account_id", nullable = false)
    private Long accountId;

    /** Navigation to parent Card. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_number", insertable = false, updatable = false)
    private Card card;

    /** Navigation to parent Customer. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", insertable = false, updatable = false)
    private Customer customer;

    /** Navigation to parent Account. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", insertable = false, updatable = false)
    private Account account;
}
