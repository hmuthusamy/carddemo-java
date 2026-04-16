package com.carddemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "statements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Statement {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "statement_seq")
    @SequenceGenerator(name = "statement_seq", sequenceName = "statement_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "statement_date", nullable = false)
    private LocalDate statementDate;

    @Column(name = "opening_balance", precision = 12, scale = 2)
    private BigDecimal openingBalance;

    @Column(name = "closing_balance", precision = 12, scale = 2)
    private BigDecimal closingBalance;

    @Column(name = "total_charges", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalCharges = BigDecimal.ZERO;

    @Column(name = "total_credits", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalCredits = BigDecimal.ZERO;

    @Column(name = "minimum_payment", precision = 12, scale = 2)
    private BigDecimal minimumPayment;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "GENERATED";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
