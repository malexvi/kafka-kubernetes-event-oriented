package com.sfr.sfr_worker_region.infrastructure.persistence;

import com.sfr.sfr_worker_region.domain.entity.RegionProcessing;
import com.sfr.sfr_worker_region.ports.JpaRegionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RegionProcessingPersistenceAdapter implements JpaRegionRepositoryPort {

    private final JpaRegionProcessingRepository repository;

    @Override
    public RegionProcessing save(RegionProcessing regionProcessing) {
        return repository.save(regionProcessing);
    }
}