package com.sfr.sfr_worker_region.infrastructure.client.dto;

public record AddressResponse(
        String cep,
        String logradouro,
        String bairro,
        String localidade,
        String uf,
        String erro
) {}