package com.sfr.sfr_orchestrator_api.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sfr.sfr_orchestrator_api.application.dto.PackageDeliveryRequest;
import com.sfr.sfr_orchestrator_api.application.event.DeliveryRequestedEvent;
import com.sfr.sfr_orchestrator_api.application.mapper.PackageDeliveryMapper;
import com.sfr.sfr_orchestrator_api.application.port.EventPublisher;
import com.sfr.sfr_orchestrator_api.domain.entity.PackageDelivery;
import com.sfr.sfr_orchestrator_api.domain.repository.PackageDeliveryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PackageDeliveryService {

    private final PackageDeliveryRepository repository;
    private final EventPublisher eventPublisher;

    public void create(PackageDeliveryRequest packageDeliveryRequest) {

        PackageDelivery delivery = PackageDeliveryMapper.toDelivery(packageDeliveryRequest, UUID.randomUUID());

        var savedDelivery = repository.save(delivery);

        var eventToPublish = DeliveryRequestedEvent.from(savedDelivery);

        eventPublisher.publish(eventToPublish);
    }
}
