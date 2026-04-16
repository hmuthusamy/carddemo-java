package com.carddemo.repository;

import com.carddemo.model.Account;
import com.carddemo.model.CreditCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CreditCardRepository extends JpaRepository<CreditCard, Long> {
    Optional<CreditCard> findByCardNumber(String cardNumber);
    List<CreditCard> findByAccount(Account account);
    boolean existsByCardNumber(String cardNumber);
}
