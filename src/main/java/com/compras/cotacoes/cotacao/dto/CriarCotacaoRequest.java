package com.compras.cotacoes.cotacao.dto;

import com.compras.cotacoes.cotacao.StatusCotacao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CriarCotacaoRequest(
        @Schema(example = "1")
        @NotNull(message = "{validacao.obrigatorio}")
        Long produtoId,

        @Schema(example = "30")
        @NotNull(message = "{validacao.obrigatorio}")
        @Positive(message = "{validacao.positivo}")
        @Digits(integer = 15, fraction = 4, message = "{validacao.digitos}")
        BigDecimal quantidade,

        @Schema(description = "RASCUNHO (padrão) ou ABERTA", example = "RASCUNHO")
        StatusCotacao status,

        @Schema(example = "2026-12-15")
        LocalDate dataLimite,

        @Size(max = 500, message = "{validacao.tamanho.maximo}")
        String observacao,

        @Schema(description = "Fornecedores convidados; vazio = aberta ao mercado")
        List<Long> fornecedoresIds) {
}
