package com.carddemo.repository;

import com.carddemo.model.Account;
import com.carddemo.model.Statement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatementRepository extends JpaRepository<Statement, Long> {
    List<Statement> findByAccountOrderByStatementDateDesc(Account account);
    long countByStatus(String status);
}
