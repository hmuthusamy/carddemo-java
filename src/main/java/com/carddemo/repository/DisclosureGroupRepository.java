package com.carddemo.repository;

import com.carddemo.model.DisclosureGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link DisclosureGroup}.
 *
 * Replaces DISCGRP-FILE random-access reads in CBACT04C (1200-GET-INTEREST-RATE).
 * Fallback to 'DEFAULT' group mirrors 1200-A-GET-DEFAULT-INT-RATE logic.
 */
@Repository
public interface DisclosureGroupRepository
        extends JpaRepository<DisclosureGroup, DisclosureGroup.DisclosureGroupKey> {

    /**
     * Look up the disclosure group by its three-part composite key.
     * Returns empty when key is not found (DISCGRP-STATUS = '23' in COBOL).
     */
    Optional<DisclosureGroup> findByIdDisAcctGroupIdAndIdDisTranTypeCdAndIdDisTranCatCd(
            String disAcctGroupId,
            String disTranTypeCd,
            Integer disTranCatCd);
}
