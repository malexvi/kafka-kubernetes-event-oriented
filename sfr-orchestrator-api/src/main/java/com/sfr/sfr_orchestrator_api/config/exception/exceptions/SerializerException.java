package com.sfr.sfr_orchestrator_api.config.exception.exceptions;

import org.springframework.http.HttpStatus;

import com.sfr.sfr_orchestrator_api.config.exception.CustomHttpException;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = false)
public class SerializerException extends CustomHttpException {

    public SerializerException(String title, String detail) {
        super(title, detail, HttpStatus.BAD_REQUEST);
    }

}