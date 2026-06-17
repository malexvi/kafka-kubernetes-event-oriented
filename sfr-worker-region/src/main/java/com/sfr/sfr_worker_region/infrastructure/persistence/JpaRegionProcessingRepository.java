package com.sfr.sfr_worker_region.infrastructure.persistence;

import com.sfr.sfr_worker_region.domain.entity.RegionProcessing;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface JpaRegionProcessingRepository extends JpaRepository<RegionProcessing, UUID> {
}