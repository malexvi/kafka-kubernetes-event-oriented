package com.sfr.sfr_orchestrator_api.api.controller;

import com.sfr.sfr_orchestrator_api.application.dto.PackageDeliveryRequest;
import com.sfr.sfr_orchestrator_api.application.service.PackageDeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.sfr.sfr_orchestrator_api.config.constants.PathConstants.DELIVERY_API_PATH;

@RestController
@RequestMapping(DELIVERY_API_PATH)
@RequiredArgsConstructor
public class PackageDeliveryController {

    private final PackageDeliveryService packageDeliveryService;

    @PostMapping
    public ResponseEntity<Void> create(
            @RequestAttribute("correlationId") UUID correlationId,
            @Valid @RequestBody PackageDeliveryRequest request) {

        packageDeliveryService.create(request.withCorrelationId(correlationId));

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}