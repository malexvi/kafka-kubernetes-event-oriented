package com.sfr.sfr_orchestrator_api.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PackageDeliveryRequest(
                @NotNull @Positive double height,
                @NotNull @Positive double width,
                @NotNull @Positive double length,
                @NotNull @Positive double weight,
                @NotBlank String originZipCode,
                @NotBlank String destinationZipCode) {
}
