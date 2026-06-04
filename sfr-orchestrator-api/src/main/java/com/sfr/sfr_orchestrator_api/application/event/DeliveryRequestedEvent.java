package com.sfr.sfr_orchestrator_api.application.event;

import java.time.Instant;
import java.util.UUID;

import com.sfr.sfr_orchestrator_api.domain.entity.PackageDelivery;

public record DeliveryRequestedEvent(
        UUID correlationId,
        UUID orderId,
        Double weight,
        Double height,
        Double width,
        Double length,
        String originZipCode,
        String destinationZipCode,
        Instant timestamp) implements Event {

    public static DeliveryRequestedEvent from(PackageDelivery delivery) {
        return new DeliveryRequestedEvent(
                delivery.getCorrelationId(),
                delivery.getOrderId(),
                delivery.getDimension().getWeight(),
                delivery.getDimension().getHeight(),
                delivery.getDimension().getWidth(),
                delivery.getDimension().getLength(),
                delivery.getRegion().getOriginZipCode(),
                delivery.getRegion().getDestinationZipCode(),
                delivery.getTimestamp());
    }
}
