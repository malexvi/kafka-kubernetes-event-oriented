package com.sfr.sfr_orchestrator_api.config.constants;

public class ErrorMessagesConstants {
    private ErrorMessagesConstants(){}

    public static final String ERROR_SERIALIZER_OUTBOX_TITLE = "Falha de Serialização";
    public static final String ERROR_SERIALIZER_OUTBOX_DETAIL = "Erro ao converter o evento de integração para o formato JSON. Causa: %s";
}