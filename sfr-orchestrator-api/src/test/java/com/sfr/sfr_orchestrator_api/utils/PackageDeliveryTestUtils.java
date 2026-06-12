package com.sfr.sfr_orchestrator_api.utils;

import com.sfr.sfr_orchestrator_api.application.dto.PackageDeliveryRequest;
import com.sfr.sfr_orchestrator_api.domain.entity.OutboxEvent;
import com.sfr.sfr_orchestrator_api.domain.entity.PackageDelivery;
import com.sfr.sfr_orchestrator_api.domain.entity.PackageDimension;
import com.sfr.sfr_orchestrator_api.domain.entity.PackageRegion;
import com.sfr.sfr_orchestrator_api.domain.enums.DeliveryStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class PackageDeliveryTestUtils {

    private PackageDeliveryTestUtils() {}

    public static PackageDeliveryRequest createValidRequest(UUID correlationId) {
        return new PackageDeliveryRequest(
                correlationId,
                15.5, 20.0, 30.0, 2.5, "01001000", "20001000"
        );
    }

    public static PackageDelivery createSavedEntity(PackageDeliveryRequest request, UUID orderId, UUID correlationId) {
        return PackageDelivery.builder()
                .orderId(orderId)
                .correlationId(correlationId)
                .dimension(PackageDimension.builder()
                        .height(request.height())
                        .width(request.width())
                        .length(request.length())
                        .weight(request.weight())
                        .build())
                .region(PackageRegion.builder()
                        .originZipCode(request.originZipCode())
                        .destinationZipCode(request.destinationZipCode())
                        .build())
                .status(DeliveryStatus.STARTED)
                .build();
    }

    public static PackageDeliveryRequest getValidRequestBody() {
        return new PackageDeliveryRequest(
                null, // O Jackson recebe nulo do HTTP; o interceptor/controller vai preencher
                15.5,
                20.0,
                30.0,
                2.5,
                "01001000",
                "20001000"
        );
    }

    public static PackageDeliveryRequest getRequestBodyWithCepInvalid() {
        return new PackageDeliveryRequest(
                null,
                15.5,
                20.0,
                30.0,
                2.5,
                "0100-1000", // CEP fora do padrão estrito de 8 dígitos
                "20001000"
        );
    }

    public static OutboxEvent getOutboxEvent(String topic) {
        return OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateId(UUID.randomUUID().toString())
                .topic(topic)
                .processed(false)
                .createdAt(LocalDateTime.now())
                .payload("""
                        {
                            "height": 15.5,
                            "width": 20.0,
                            "length": 30.0,
                            "weight": 2.5,
                            "originZipCode": "01001000",
                            "destinationZipCode": "20001000"
                        }
                        """)
                .build();
    }
}