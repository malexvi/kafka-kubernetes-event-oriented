package com.sfr.sfr_orchestrator_api.api.controller;

import com.sfr.sfr_orchestrator_api.application.dto.PackageDeliveryRequest;
import com.sfr.sfr_orchestrator_api.application.service.PackageDeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

//@RestController
//RequestMapping("/api/delivery")
@RequiredArgsConstructor
public class PackageDeliveryControllerAbordagem1 {

    private final PackageDeliveryService packageDeliveryService;

    // Abordagem 1
    @PostMapping
    public ResponseEntity<Void> create(
            @RequestHeader(value = "X-Correlation-ID", required = false) String headerCorrelationId,
            @Valid @RequestBody PackageDeliveryRequest request) {

        UUID correlationId = (headerCorrelationId != null && !headerCorrelationId.isBlank())
                ? UUID.fromString(headerCorrelationId)
                : UUID.randomUUID();

        MDC.put("correlationId", correlationId.toString());

        try {
            var requestWithCorrelation = new PackageDeliveryRequest(
                    correlationId,
                    request.height(),
                    request.width(),
                    request.length(),
                    request.weight(),
                    request.originZipCode(),
                    request.destinationZipCode()
            );

            packageDeliveryService.create(requestWithCorrelation);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .header("X-Correlation-ID", correlationId.toString())
                    .build();

        } finally {
            MDC.clear();
        }
    }
}