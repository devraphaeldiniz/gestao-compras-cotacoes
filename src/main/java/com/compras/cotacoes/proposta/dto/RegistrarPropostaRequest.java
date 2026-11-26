package com.compras.cotacoes.proposta.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RegistrarPropostaRequest(
        @Schema(example = "1")
        @NotNull(message = "{validacao.obrigatorio}")
        Long fornecedorId,

        @Schema(example = "24.90")
        @NotNull(message = "{validacao.obrigatorio}")
        @Positive(message = "{validacao.positivo}")
        BigDecimal precoUnitario,

        @Schema(example = "35.00")
        @PositiveOrZero(message = "{validacao.nao.negativo}")
        BigDecimal valorFrete,

        @Schema(description = "Percentual de imposto sobre a mercadoria", example = "12")
        @PositiveOrZero(message = "{validacao.nao.negativo}")
        @DecimalMax(value = "100", message = "{validacao.percentual}")
        BigDecimal impostoPercentual,

        @Schema(example = "0")
        @PositiveOrZero(message = "{validacao.nao.negativo}")
        BigDecimal valorImpostoFixo,

        @Schema(example = "5")
        @NotNull(message = "{validacao.obrigatorio}")
        @PositiveOrZero(message = "{validacao.nao.negativo}")
        Integer prazoEntregaDias,

        @Schema(example = "2026-12-31")
        @NotNull(message = "{validacao.obrigatorio}")
        LocalDate dataValidade) {
}
