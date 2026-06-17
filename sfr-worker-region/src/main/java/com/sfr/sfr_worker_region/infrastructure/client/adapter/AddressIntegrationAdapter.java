package com.sfr.sfr_worker_region.infrastructure.client.adapter;

import com.sfr.sfr_worker_region.infrastructure.client.dto.ViaCepClient;
import com.sfr.sfr_worker_region.ports.AddressIntegrationPort;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AddressIntegrationAdapter implements AddressIntegrationPort {

    private final ViaCepClient viaCepClient;

    @Override
    public String getState(String zipCode) {
        try {
            String cleanZipCode = zipCode.replace("-", "");
            var response = viaCepClient.consultarCep(cleanZipCode);

            // "erro" and "uf" are kept as they map directly to the external ViaCEP JSON response fields
            if (response.erro() != null && response.erro().equals("true")) {
                throw new IllegalArgumentException("Zip code not found in the database: " + zipCode);
            }

            return response.uf();

        } catch (Exception e) {
            log.error("Error integrating with Zip Code API for value {}: {}", zipCode, e.getMessage());
            throw new RuntimeException("Failed to query logistics network integration.");
        }
    }
}