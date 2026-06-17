package com.sfr.sfr_worker_region.infrastructure.kafka;

import com.sfr.sfr_worker_region.domain.entity.RegionProcessing;
import com.sfr.sfr_worker_region.infrastructure.kafka.avro.RegionDefinedEvent;
import com.sfr.sfr_worker_region.ports.RegionEventPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaRegionEventPublisherAdapter implements RegionEventPublisherPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.package-delivery-response-topic:package-delivery-response-topic}")
    private String responseTopic;

    @Override
    public void publish(RegionProcessing processing) {
        log.info("[Kafka Adapter] Preparing to send RegionDefinedEvent to topic {} for OrderID: {}",
                responseTopic, processing.getOrderId());

        // Mapeamento da Entidade de Domínio para o Contrato de Infraestrutura (Avro)
        RegionDefinedEvent responseEvent = RegionDefinedEvent.newBuilder()
                .setOrderId(UUID.fromString(processing.getOrderId().toString()))
                .setCorrelationId(UUID.fromString(processing.getCorrelationId().toString()))
                .setRegion(processing.getDefinedRegion())
                .setCubedWeight(processing.getCubedWeight())
                .setStatus(processing.getStatus())
                .build();

        // Envio da mensagem (ainda amparado pela transação iniciada na Service)
        kafkaTemplate.send(responseTopic, processing.getOrderId().toString(), responseEvent);
    }
}