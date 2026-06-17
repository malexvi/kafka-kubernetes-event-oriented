package com.sfr.sfr_worker_region.ports;

import com.sfr.sfr_worker_region.domain.entity.RegionProcessing;

public interface JpaRegionRepositoryPort {
    RegionProcessing save(RegionProcessing regionProcessing);
}