package com.sfr.sfr_orchestrator_api.domain.entity;

import java.time.Instant;
import java.util.UUID;

import com.sfr.sfr_orchestrator_api.domain.enums.DeliveryStatus;
import com.sfr.sfr_orchestrator_api.domain.enums.DeliveryType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "package_delivery")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackageDelivery {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID orderId;

    private UUID correlationId;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "dimension_id", referencedColumnName = "id")
    private PackageDimension dimension;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "region_id", referencedColumnName = "id")
    private PackageRegion region;

    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;

    @Enumerated(EnumType.STRING)
    private DeliveryType deliveryType;

    private Instant timestamp;
}
