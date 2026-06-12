package com.sfr.sfr_orchestrator_api.application.dto;

import com.sfr.sfr_orchestrator_api.api.commom.validation.Cep;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record PackageDeliveryRequest(
        UUID correlationId,
        @NotNull @Positive double height,
        @NotNull @Positive double width,
        @NotNull @Positive double length,
        @NotNull @Positive double weight,
        @NotBlank @Cep String originZipCode,
        @NotBlank @Cep String destinationZipCode) {

    public PackageDeliveryRequest withCorrelationId(UUID id) {
        return new PackageDeliveryRequest(id, this.height, this.width, this.length, this.weight, this.originZipCode, this.destinationZipCode);
    }
}