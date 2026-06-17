package com.sfr.sfr_worker_region.service;

import java.util.UUID;

import com.sfr.sfr_worker_region.ports.AddressIntegrationPort;
import com.sfr.sfr_worker_region.ports.JpaRegionRepositoryPort;
import com.sfr.sfr_worker_region.ports.RegionEventPublisherPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sfr.sfr_worker_region.domain.entity.RegionProcessing;
import com.sfr.sfr_worker_region.infrastructure.kafka.avro.RequestStartedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessRegionService {

    private final JpaRegionRepositoryPort jpaRegionRepositoryPort;
    private final AddressIntegrationPort addressIntegrationPort;
    private final RegionEventPublisherPort regionEventPublisherPort;

    @Transactional
    public void process(RequestStartedEvent event) {
        UUID orderId = UUID.fromString(event.getOrderId().toString());
        UUID correlationId = UUID.fromString(event.getCorrelationId().toString());

        log.info("[Service Region] Executing business rules for OrderID: {}", orderId);

        String definedRegion = mapGeographicRegion(
                event.getOriginZipCode(),
                event.getDestinationZipCode()
        );

        double cubedWeight = calculateCubedWeight(
                event.getHeight(),
                event.getWidth(),
                event.getLength()
        );

        log.info("[Service Region] Region: {} | Cubed Weight: {}kg", definedRegion, cubedWeight);

        RegionProcessing regionProcessing = RegionProcessing.builder()
                .orderId(orderId)
                .correlationId(correlationId)
                .originZipCode(event.getOriginZipCode())
                .destinationZipCode(event.getDestinationZipCode())
                .definedRegion(definedRegion)
                .cubedWeight(cubedWeight)
                .status("SUCCESS")
                .build();

        jpaRegionRepositoryPort.save(regionProcessing);

        regionEventPublisherPort.publish(regionProcessing);
    }

    private String mapGeographicRegion(String originZipCode, String destinationZipCode) {
        log.info("Consulting Logistics Network for Origin: {} and Destination: {}", originZipCode, destinationZipCode);

        String originState = addressIntegrationPort.getState(originZipCode);
        String destinationState = addressIntegrationPort.getState(destinationZipCode);

        if (originState.equals(destinationState)) {
            return "SAME_REGION";
        }
        return "DISTANT_REGION";
    }

    private double calculateCubedWeight(double height, double width, double length) {
        double volumeCubicMeters = (height * width * length) / 1_000_000.0;
        return volumeCubicMeters * 300.0;
    }
}