package com.carddemo.repository;

import com.carddemo.model.AccountData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link AccountData}.
 * Used by Cbtrn02cService to look up and persist account balance updates.
 */
@Repository
public interface AccountDataRepository extends JpaRepository<AccountData, Long> {
}
