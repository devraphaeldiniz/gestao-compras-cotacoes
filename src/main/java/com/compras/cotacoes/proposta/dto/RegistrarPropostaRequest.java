package com.compras.cotacoes.proposta.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RegistrarPropostaRequest(
        @Schema(example = "1")
        @NotNull(message = "{validacao.obrigatorio}")
        Long fornecedorId,

        @Schema(example = "24.90")
        @NotNull(message = "{validacao.obrigatorio}")
        @Positive(message = "{validacao.positivo}")
        @Digits(integer = 15, fraction = 4, message = "{validacao.digitos}")
        BigDecimal precoUnitario,

        @Schema(example = "35.00")
        @PositiveOrZero(message = "{validacao.nao.negativo}")
        @Digits(integer = 13, fraction = 2, message = "{validacao.digitos}")
        BigDecimal valorFrete,

        @Schema(description = "Percentual de imposto sobre a mercadoria", example = "12")
        @PositiveOrZero(message = "{validacao.nao.negativo}")
        @DecimalMax(value = "100", message = "{validacao.percentual}")
        @Digits(integer = 3, fraction = 4, message = "{validacao.digitos}")
        BigDecimal impostoPercentual,

        @Schema(example = "0")
        @PositiveOrZero(message = "{validacao.nao.negativo}")
        @Digits(integer = 13, fraction = 2, message = "{validacao.digitos}")
        BigDecimal valorImpostoFixo,

        @Schema(example = "5")
        @NotNull(message = "{validacao.obrigatorio}")
        @PositiveOrZero(message = "{validacao.nao.negativo}")
        @Max(value = 3650, message = "{validacao.maximo}")
        Integer prazoEntregaDias,

        @Schema(example = "2026-12-31")
        @NotNull(message = "{validacao.obrigatorio}")
        LocalDate dataValidade) {
}
