package com.sfr.sfr_orchestrator_api.config.exception;


import com.sfr.sfr_orchestrator_api.config.exception.exceptions.SerializerException;

import static com.sfr.sfr_orchestrator_api.config.constants.ErrorMessagesConstants.ERROR_SERIALIZER_OUTBOX_DETAIL;
import static com.sfr.sfr_orchestrator_api.config.constants.ErrorMessagesConstants.ERROR_SERIALIZER_OUTBOX_TITLE;

public class ExceptionFactory {

    private ExceptionFactory() {}

    public static SerializerException serializerException(Throwable cause) {
        String detail = String.format(ERROR_SERIALIZER_OUTBOX_DETAIL, cause.getMessage());
        return new SerializerException(ERROR_SERIALIZER_OUTBOX_TITLE, detail);
    }
}