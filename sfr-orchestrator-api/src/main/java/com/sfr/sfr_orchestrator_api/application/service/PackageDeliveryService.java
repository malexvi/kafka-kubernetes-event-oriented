package com.sfr.sfr_orchestrator_api.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfr.sfr_orchestrator_api.application.dto.PackageDeliveryRequest;
import com.sfr.sfr_orchestrator_api.application.event.DeliveryRequestedEvent;
import com.sfr.sfr_orchestrator_api.application.mapper.OutboxEventMapper;
import com.sfr.sfr_orchestrator_api.application.mapper.PackageDeliveryMapper;
import com.sfr.sfr_orchestrator_api.application.port.JpaRepositoryPort;
import com.sfr.sfr_orchestrator_api.application.port.OutboxRepositoryPort;
import com.sfr.sfr_orchestrator_api.config.KafkaTopicsProperties;
import com.sfr.sfr_orchestrator_api.config.exceptions.SerializerException;
import com.sfr.sfr_orchestrator_api.domain.entity.OutboxEvent;
import com.sfr.sfr_orchestrator_api.domain.entity.PackageDelivery;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PackageDeliveryService {

    private final KafkaTopicsProperties topicProperties;
    private final JpaRepositoryPort jpaRepositoryPort;
    private final OutboxRepositoryPort outboxRepositoryPort;
    private final ObjectMapper objectMapper;

    @Transactional
    public void create(PackageDeliveryRequest packageDeliveryRequest) {

        PackageDelivery delivery = PackageDeliveryMapper.toDelivery(packageDeliveryRequest, UUID.randomUUID());

        PackageDelivery savedDelivery = jpaRepositoryPort.save(delivery);

        var eventToPublish = DeliveryRequestedEvent.from(savedDelivery);

        try {
            String jsonPayload = objectMapper.writeValueAsString(eventToPublish);
            OutboxEvent outboxEvent = OutboxEventMapper.toOutboxEvent(savedDelivery,
                    topicProperties.getPackageDelivery(), jsonPayload);

            outboxRepositoryPort.save(outboxEvent);

        } catch (Exception e) {
            throw new SerializerException("", "");
        }
    }
}
