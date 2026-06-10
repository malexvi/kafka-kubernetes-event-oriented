package com.sfr.sfr_orchestrator_api.api.commom.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = CepValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface Cep {

    String message() default "The ZIP code must contain exactly 8 numeric digits";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
