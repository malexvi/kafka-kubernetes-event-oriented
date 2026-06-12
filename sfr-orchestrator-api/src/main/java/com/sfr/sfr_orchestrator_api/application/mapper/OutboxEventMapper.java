package com.sfr.sfr_orchestrator_api.application.mapper;

import java.time.LocalDateTime;
import java.util.UUID;

import com.sfr.sfr_orchestrator_api.domain.entity.OutboxEvent;
import com.sfr.sfr_orchestrator_api.domain.entity.PackageDelivery;

public class OutboxEventMapper {
    public static OutboxEvent toOutboxEvent(PackageDelivery packageDelivery, String topic, String jsonPayload) {
        return OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateId(packageDelivery.getOrderId().toString())
                .topic(topic)
                .payload(jsonPayload)
                .processed(false)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
