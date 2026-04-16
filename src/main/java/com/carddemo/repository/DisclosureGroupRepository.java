package com.carddemo.repository;

import com.carddemo.model.DisclosureGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for DisclosureGroup — mirrors DISCGRP-FILE (CVTRA02Y).
 * Supports random key access used in CBACT04C paragraph 1200-GET-INTEREST-RATE.
 */
@Repository
public interface DisclosureGroupRepository extends JpaRepository<DisclosureGroup, DisclosureGroup.DisclosureGroupKey> {

    /**
     * Find by composite key parts — mirrors READ DISCGRP-FILE by FD-DISCGRP-KEY.
     */
    Optional<DisclosureGroup> findById_DisAcctGroupIdAndId_DisTranTypeCdAndId_DisTranCatCd(
            String disAcctGroupId, String disTranTypeCd, Integer disTranCatCd);
}
