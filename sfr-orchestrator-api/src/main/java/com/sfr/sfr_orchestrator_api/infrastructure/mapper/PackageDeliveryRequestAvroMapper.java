package com.sfr.sfr_orchestrator_api.infrastructure.mapper;

import org.springframework.stereotype.Component;

import com.sfr.sfr_orchestrator_api.application.event.DeliveryRequestedEvent;
import com.sfr.sfr_orchestrator_api.infrastructure.kafka.avro.RequestStartedEvent;

@Component
public class PackageDeliveryRequestAvroMapper {

    public RequestStartedEvent map(DeliveryRequestedEvent event) {
        return RequestStartedEvent.newBuilder()
                .setCorrelationId(event.correlationId())
                .setOrderId(event.orderId())
                .setWeight(event.weight())
                .setHeight(event.height())
                .setWidth(event.width())
                .setLength(event.length())
                .setOriginZipCode(event.originZipCode())
                .setDestinationZipCode(event.destinationZipCode())
                .build();
    }
}
