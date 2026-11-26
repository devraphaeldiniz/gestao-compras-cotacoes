package com.compras.cotacoes.proposta.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record ItemPropostaRequest(
        @Schema(description = "Produto do item da cotação", example = "1")
        @NotNull(message = "{validacao.obrigatorio}")
        Long produtoId,

        @Schema(example = "24.90")
        @NotNull(message = "{validacao.obrigatorio}")
        @Positive(message = "{validacao.positivo}")
        @Digits(integer = 15, fraction = 4, message = "{validacao.digitos}")
        BigDecimal precoUnitario) {
}
