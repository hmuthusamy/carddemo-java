package com.carddemo.repository;

import com.carddemo.model.Account;
import com.carddemo.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountNumber(String accountNumber);
    List<Account> findByCustomer(Customer customer);
    boolean existsByAccountNumber(String accountNumber);
}
