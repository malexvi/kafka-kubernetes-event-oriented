package com.sfr.sfr_orchestrator_api.infrastructure.kafka.scheduler;

import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional; // Adicionado

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfr.sfr_orchestrator_api.application.event.DeliveryRequestedEvent;
import com.sfr.sfr_orchestrator_api.application.port.OutboxRepositoryPort;
import com.sfr.sfr_orchestrator_api.domain.entity.OutboxEvent;
import com.sfr.sfr_orchestrator_api.infrastructure.kafka.avro.RequestStartedEvent;
import com.sfr.sfr_orchestrator_api.infrastructure.mapper.PackageDeliveryRequestAvroMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelayScheduler {

    private final OutboxRepositoryPort outboxRepositoryPort;
    private final KafkaTemplate<String, RequestStartedEvent> kafkaTemplate;
    private final PackageDeliveryRequestAvroMapper avroMapper;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 200)
    @Transactional
    public void pullAndPublish() {
        List<OutboxEvent> unprocessedEvents = outboxRepositoryPort.findUnprocessedEvents();

        if (unprocessedEvents.isEmpty()) {
            return;
        }

        log.info("Encontrados {} eventos na Outbox para processar.", unprocessedEvents.size());

        for (OutboxEvent event : unprocessedEvents) {
            try {
                DeliveryRequestedEvent requestedEvent = objectMapper.readValue(event.getPayload(),
                        DeliveryRequestedEvent.class);

                RequestStartedEvent avro = avroMapper.map(requestedEvent);

                kafkaTemplate.send(event.getTopic(), event.getAggregateId(), avro).get();

                event.setProcessed(true);
                outboxRepositoryPort.save(event);

                log.info("Evento da Outbox enviado com sucesso para o Kafka. ID: {}", event.getId());

            } catch (Exception e) {
                log.error(
                        "Erro ao despachar evento da Outbox {}. O processo tentará novamente no próximo ciclo. Erro: {}",
                        event.getId(), e.getMessage());
                break;
            }
        }
    }
}