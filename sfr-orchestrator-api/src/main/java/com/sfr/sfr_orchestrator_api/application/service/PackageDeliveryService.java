package com.sfr.sfr_orchestrator_api.application.service;

import com.sfr.sfr_orchestrator_api.application.dto.PackageDeliveryRequest;
import com.sfr.sfr_orchestrator_api.application.event.DeliveryRequestedEvent;
import com.sfr.sfr_orchestrator_api.application.mapper.OutboxEventMapper;
import com.sfr.sfr_orchestrator_api.application.mapper.PackageDeliveryMapper;
import com.sfr.sfr_orchestrator_api.application.port.JpaRepositoryPort;
import com.sfr.sfr_orchestrator_api.application.port.OutboxRepositoryPort;
import com.sfr.sfr_orchestrator_api.config.KafkaTopicsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PackageDeliveryService {

    private final KafkaTopicsProperties topicProperties;
    private final JpaRepositoryPort jpaRepositoryPort;
    private final OutboxRepositoryPort outboxRepositoryPort;
    private final OutboxEventMapper outboxEventMapper;

    @Transactional
    public void create(PackageDeliveryRequest request) {

        var packageDelivery = PackageDeliveryMapper.toDelivery(request);

        var persistedDelivery = jpaRepositoryPort.save(packageDelivery);

        var deliveryEvent = DeliveryRequestedEvent.from(persistedDelivery);

        var outboxEvent = outboxEventMapper.toOutboxEvent(
                persistedDelivery,
                topicProperties.getPackageDeliveryTopic(),
                deliveryEvent
        );

        outboxRepositoryPort.save(outboxEvent);
    }
}