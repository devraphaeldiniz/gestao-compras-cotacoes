package com.compras.cotacoes.shared.validacao;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** CPF (11 dígitos) ou CNPJ (14 dígitos) com dígitos verificadores corretos; aceita com ou sem pontuação. */
@Documented
@Constraint(validatedBy = DocumentoValidoValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface DocumentoValido {

    String message() default "{validacao.documento}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
