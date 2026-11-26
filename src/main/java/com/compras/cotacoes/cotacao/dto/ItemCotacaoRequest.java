package com.compras.cotacoes.cotacao.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record ItemCotacaoRequest(
        @Schema(example = "1")
        @NotNull(message = "{validacao.obrigatorio}")
        Long produtoId,

        @Schema(example = "30")
        @NotNull(message = "{validacao.obrigatorio}")
        @Positive(message = "{validacao.positivo}")
        @Digits(integer = 15, fraction = 4, message = "{validacao.digitos}")
        BigDecimal quantidade) {
}
