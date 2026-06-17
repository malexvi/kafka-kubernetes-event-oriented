package com.sfr.sfr_worker_region.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "region_processing")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegionProcessing {

    @Id
    private UUID orderId;
    private UUID correlationId;
    private String originZipCode;
    private String destinationZipCode;
    private String definedRegion;
    private Double cubedWeight;
    private String status;
}