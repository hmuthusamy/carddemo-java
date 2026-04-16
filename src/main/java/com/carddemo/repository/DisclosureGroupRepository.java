package com.carddemo.repository;

import com.carddemo.model.DisclosureGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA Repository for DisclosureGroup entity (VSAM DISCGRP KSDS).
 * Mapped from COBOL copybook CVTRA02Y.cpy
 */
@Repository
public interface DisclosureGroupRepository extends JpaRepository<DisclosureGroup, String> {
}
