package com.sfr.sfr_orchestrator_api.infrastructure.kafka.producer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import com.sfr.sfr_orchestrator_api.application.event.DeliveryRequestedEvent;
import com.sfr.sfr_orchestrator_api.application.event.Event;
import com.sfr.sfr_orchestrator_api.application.port.EventPublisher;
import com.sfr.sfr_orchestrator_api.infrastructure.kafka.avro.RequestStartedEvent;
import com.sfr.sfr_orchestrator_api.infrastructure.mapper.PackageDeliveryRequestAvroMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafakaEventPublisher implements EventPublisher {

    @Value("${kafka.topic.package-delivery}")
    private String topic;

    private final PackageDeliveryRequestAvroMapper avroMapper;

    private final KafkaTemplate<String, RequestStartedEvent> kafkaTemplate;

    @Override
    public void publish(Event event) {
        if (event instanceof DeliveryRequestedEvent requested) {
            RequestStartedEvent avro = avroMapper.map(requested);

            kafkaTemplate.send(
                    topic,
                    avro.getOrderId().toString(),
                    avro).whenComplete((result, ex) -> {
                        if (ex != null) {
                            handleFailure(avro.getOrderId().toString(), avro, ex);
                        } else {
                            handleSuccess(avro.getOrderId().toString(), result);
                        }
                    });
        }
    }

    private void handleSuccess(String key, SendResult<String, RequestStartedEvent> result) {
        log.info("event published! Package Delivery: {}, Partition: {}, Offset: {}",
                key,
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
    }

    private void handleFailure(String key, RequestStartedEvent event, Throwable ex) {
        log.error("Failed published event: {}. Object: {}. Error: {}",
                key, event, ex.getMessage(), ex);
    }

}
