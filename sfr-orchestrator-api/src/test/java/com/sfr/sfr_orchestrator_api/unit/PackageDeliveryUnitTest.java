package com.sfr.sfr_orchestrator_api.unit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfr.sfr_orchestrator_api.application.dto.PackageDeliveryRequest;
import com.sfr.sfr_orchestrator_api.application.event.DeliveryRequestedEvent;
import com.sfr.sfr_orchestrator_api.application.mapper.PackageDeliveryMapper;
import com.sfr.sfr_orchestrator_api.application.port.JpaRepositoryPort;
import com.sfr.sfr_orchestrator_api.application.port.OutboxRepositoryPort;
import com.sfr.sfr_orchestrator_api.application.service.PackageDeliveryService;
import com.sfr.sfr_orchestrator_api.config.KafkaTopicsProperties;
import com.sfr.sfr_orchestrator_api.domain.entity.OutboxEvent;
import com.sfr.sfr_orchestrator_api.domain.entity.PackageDelivery;

@ExtendWith(MockitoExtension.class)
public class PackageDeliveryUnitTest {

    @Mock
    private KafkaTopicsProperties topicProperties;
    @Mock
    private JpaRepositoryPort jpaRepositoryPort;
    @Mock
    private OutboxRepositoryPort outboxRepositoryPort;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    PackageDeliveryService packageDeliveryService;

    @Test
    void shouldCreateNewPackageDelivery() throws JsonProcessingException {

        // GIVE
        var delivery = new PackageDeliveryRequest(
                15.5,
                20.0,
                30.0,
                2.5,
                "01001000",
                "20001000");

        UUID correlationId = UUID.fromString("e4536e98-3280-4711-93b0-e00a25b569f8");
        UUID orderId = UUID.fromString("4405857f-af87-470c-88f9-18056b3fd745");

        var savedPackageDelivery = PackageDeliveryMapper.toDelivery(delivery, correlationId);
        savedPackageDelivery.setOrderId(orderId);

        var topicExpect = "package-delivery-topic";

        String jsonPayload = """
                {
                    "height": 15.5,
                    "width": 20.0,
                    "length": 30.0,
                    "weight": 2.5,
                    "originZipCode": "01001000",
                    "destinationZipCode": "20001000"
                }
                """;

        // WHEN
        when(jpaRepositoryPort.save(any(PackageDelivery.class))).thenReturn(savedPackageDelivery);
        when(topicProperties.getPackageDelivery()).thenReturn(topicExpect);
        when(objectMapper.writeValueAsString(any(DeliveryRequestedEvent.class))).thenReturn(jsonPayload);

        // THEN
        packageDeliveryService.create(delivery);

        verify(jpaRepositoryPort, times(1)).save(any(PackageDelivery.class));
        verify(topicProperties, times(1)).getPackageDelivery();
        verify(objectMapper, times(1)).writeValueAsString(any(DeliveryRequestedEvent.class));
        verify(outboxRepositoryPort, times(1)).save(any(OutboxEvent.class));
    }

}
