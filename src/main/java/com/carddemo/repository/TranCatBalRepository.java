package com.carddemo.repository;

import com.carddemo.model.TranCatBal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link TranCatBal}.
 * Used by Cbtrn02cService to implement paragraph 2700-UPDATE-TCATBAL logic.
 */
@Repository
public interface TranCatBalRepository extends JpaRepository<TranCatBal, Long> {

    /**
     * Find category balance record by composite natural key.
     * COBOL equivalent: READ TCATBAL-FILE BY KEY (acct-id + type-cd + cat-cd)
     */
    Optional<TranCatBal> findByTrancatAcctIdAndTrancatTypeCdAndTrancatCd(
            Long trancatAcctId,
            String trancatTypeCd,
            Integer trancatCd);
}
