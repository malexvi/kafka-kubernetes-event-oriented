package com.sfr.sfr_orchestrator_api.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sfr.sfr_orchestrator_api.domain.entity.PackageDelivery;

public interface JpaPackageDeliveryRepository extends JpaRepository<PackageDelivery, UUID> {

}
