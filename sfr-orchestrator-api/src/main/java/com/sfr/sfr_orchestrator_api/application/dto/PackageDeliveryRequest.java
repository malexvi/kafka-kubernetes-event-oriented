package com.sfr.sfr_orchestrator_api.application.dto;

import com.sfr.sfr_orchestrator_api.api.commom.validation.Cep;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PackageDeliveryRequest(
                @NotNull @Positive double height,
                @NotNull @Positive double width,
                @NotNull @Positive double length,
                @NotNull @Positive double weight,
                @NotBlank @Cep String originZipCode,
                @NotBlank @Cep String destinationZipCode) {
}
