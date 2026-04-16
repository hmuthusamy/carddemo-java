package com.carddemo.repository;

import com.carddemo.model.Account;
import com.carddemo.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByAccountOrderByTransactionDateDesc(Account account);
    Optional<Transaction> findByTransactionId(String transactionId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.account = :account AND t.transactionType = 'PURCHASE'")
    BigDecimal sumPurchasesByAccount(Account account);
}
