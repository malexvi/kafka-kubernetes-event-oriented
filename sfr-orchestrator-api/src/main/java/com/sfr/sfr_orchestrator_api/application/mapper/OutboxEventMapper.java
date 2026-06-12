package com.sfr.sfr_orchestrator_api.application.mapper;

import java.time.LocalDateTime;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfr.sfr_orchestrator_api.application.event.DeliveryRequestedEvent;
import com.sfr.sfr_orchestrator_api.config.exception.ExceptionFactory;
import com.sfr.sfr_orchestrator_api.domain.entity.OutboxEvent;
import com.sfr.sfr_orchestrator_api.domain.entity.PackageDelivery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxEventMapper {

    private final ObjectMapper objectMapper;

    public OutboxEvent toOutboxEvent(PackageDelivery packageDelivery, String topic, DeliveryRequestedEvent event) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(event);

            return OutboxEvent.builder()
                    .id(UUID.randomUUID())
                    .aggregateId(packageDelivery.getOrderId().toString())
                    .topic(topic)
                    .payload(jsonPayload)
                    .processed(false)
                    .createdAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            throw ExceptionFactory.serializerException(e);
        }
    }
}