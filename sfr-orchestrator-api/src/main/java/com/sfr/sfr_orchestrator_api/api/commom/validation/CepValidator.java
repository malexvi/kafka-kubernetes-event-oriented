package com.sfr.sfr_orchestrator_api.api.commom.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class CepValidator implements ConstraintValidator<Cep, String> {

    private static final Pattern CEP_PATTERN = Pattern.compile("^\\d{8}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()){
            return true;
        }

        return CEP_PATTERN.matcher(value).matches();
    }
}
