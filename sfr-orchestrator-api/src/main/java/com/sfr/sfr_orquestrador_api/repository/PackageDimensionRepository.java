package com.sfr.sfr_orquestrador_api.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sfr.sfr_orquestrador_api.domain.PackageDimension;

@Repository
public interface PackageDimensionRepository extends JpaRepository<PackageDimension, UUID> {
}
