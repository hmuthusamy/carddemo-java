package com.carddemo.repository;

import com.carddemo.model.AccountView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA Repository for AccountView (card) entity (VSAM CARD KSDS).
 * Mapped from COBOL copybook CVACT02Y.cpy
 */
@Repository
public interface AccountViewRepository extends JpaRepository<AccountView, String> {
}
