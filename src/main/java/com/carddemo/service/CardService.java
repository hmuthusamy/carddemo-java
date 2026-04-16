package com.carddemo.service;

import com.carddemo.dto.CardRequest;
import com.carddemo.model.Account;
import com.carddemo.model.CreditCard;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CreditCardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

@Service
@Transactional
public class CardService {

    private final CreditCardRepository cardRepository;
    private final AccountRepository accountRepository;

    public CardService(CreditCardRepository cardRepository, AccountRepository accountRepository) {
        this.cardRepository = cardRepository;
        this.accountRepository = accountRepository;
    }

    public CreditCard createCard(CardRequest request) {
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new RuntimeException("Account not found: " + request.getAccountId()));

        String cardNumber = generateCardNumber();

        CreditCard card = CreditCard.builder()
                .cardNumber(cardNumber)
                .account(account)
                .cardType(request.getCardType() != null ? request.getCardType() : "VISA")
                .expiryDate(request.getExpiryDate() != null ? request.getExpiryDate() : "12/2027")
                .cvv(generateCvv())
                .status("ACTIVE")
                .build();

        return cardRepository.save(card);
    }

    @Transactional(readOnly = true)
    public CreditCard getCard(Long id) {
        return cardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Card not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<CreditCard> getCardsByAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
        return cardRepository.findByAccount(account);
    }

    private String generateCardNumber() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder("4");
        for (int i = 0; i < 15; i++) {
            sb.append(random.nextInt(10));
        }
        String cardNumber = sb.toString();
        while (cardRepository.existsByCardNumber(cardNumber)) {
            sb = new StringBuilder("4");
            for (int i = 0; i < 15; i++) {
                sb.append(random.nextInt(10));
            }
            cardNumber = sb.toString();
        }
        return cardNumber;
    }

    private String generateCvv() {
        Random random = new Random();
        return String.format("%03d", random.nextInt(1000));
    }
}
