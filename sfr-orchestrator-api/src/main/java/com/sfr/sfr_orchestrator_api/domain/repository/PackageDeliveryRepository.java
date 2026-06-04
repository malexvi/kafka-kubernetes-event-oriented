package com.sfr.sfr_orchestrator_api.domain.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sfr.sfr_orchestrator_api.domain.entity.PackageDelivery;

@Repository
public interface PackageDeliveryRepository extends JpaRepository<PackageDelivery, UUID> {
}
