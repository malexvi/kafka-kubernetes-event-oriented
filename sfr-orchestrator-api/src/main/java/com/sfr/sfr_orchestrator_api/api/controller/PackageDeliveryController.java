package com.sfr.sfr_orchestrator_api.api.controller;

import com.sfr.sfr_orchestrator_api.application.dto.PackageDeliveryRequest;
import com.sfr.sfr_orchestrator_api.application.service.PackageDeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//@RestController
//RequestMapping("/api/delivery")
@RequiredArgsConstructor
public class PackageDeliveryController {

    private final PackageDeliveryService packageDeliveryService;

    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody PackageDeliveryRequest request) {
        packageDeliveryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .build();
    }
}
