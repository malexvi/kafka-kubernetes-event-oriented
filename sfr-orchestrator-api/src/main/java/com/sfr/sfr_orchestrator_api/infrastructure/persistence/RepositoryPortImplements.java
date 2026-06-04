package com.sfr.sfr_orchestrator_api.infrastructure.persistence;

import com.sfr.sfr_orchestrator_api.application.port.JpaRepositoryPort;
import com.sfr.sfr_orchestrator_api.domain.entity.PackageDelivery;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RepositoryPortImplements implements JpaRepositoryPort {

    public final JpaPackageDeliveryRepository repository;

    @Override
    public PackageDelivery save(PackageDelivery delivery) {
        return repository.save(delivery);
    }

}
