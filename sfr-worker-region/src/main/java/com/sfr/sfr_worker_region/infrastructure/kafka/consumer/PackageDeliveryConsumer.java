package com.sfr.sfr_worker_region.infrastructure.kafka.consumer;

import com.sfr.sfr_worker_region.service.ProcessRegionService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.sfr.sfr_worker_region.infrastructure.kafka.avro.RequestStartedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PackageDeliveryConsumer {

    private final ProcessRegionService processRegionService;

    @KafkaListener(
            topics = "${kafka.topic.package-delivery-topic}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(ConsumerRecord<String, RequestStartedEvent> record) {
        RequestStartedEvent event = record.value();

        log.info("[Worker Region] Recebido comando para definição de região | CorrelationID: {} | OrderID: {}",
                event.getCorrelationId(),
                event.getOrderId());

        processRegionService.process(event);
    }
}