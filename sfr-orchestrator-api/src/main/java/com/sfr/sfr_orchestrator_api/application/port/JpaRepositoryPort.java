package com.sfr.sfr_orchestrator_api.application.port;

import com.sfr.sfr_orchestrator_api.domain.entity.PackageDelivery;

public interface JpaRepositoryPort {

    PackageDelivery save(PackageDelivery delivery);

}
