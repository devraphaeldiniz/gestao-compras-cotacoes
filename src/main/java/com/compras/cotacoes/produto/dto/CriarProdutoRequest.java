package com.compras.cotacoes.produto.dto;

import com.compras.cotacoes.produto.UnidadeMedida;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CriarProdutoRequest(
        @Schema(example = "CAN-AZ-50")
        @NotBlank(message = "{validacao.obrigatorio}")
        @Size(max = 50, message = "{validacao.tamanho.maximo}")
        String sku,

        @Schema(example = "Caneta esferográfica azul - caixa com 50")
        @NotBlank(message = "{validacao.obrigatorio}")
        @Size(max = 150, message = "{validacao.tamanho.maximo}")
        String nome,

        @Schema(example = "CX")
        @NotNull(message = "{validacao.obrigatorio}")
        UnidadeMedida unidade,

        @Schema(example = "12")
        @NotNull(message = "{validacao.obrigatorio}")
        @PositiveOrZero(message = "{validacao.nao.negativo}")
        BigDecimal saldoAtual,

        @Schema(example = "10")
        @NotNull(message = "{validacao.obrigatorio}")
        @PositiveOrZero(message = "{validacao.nao.negativo}")
        BigDecimal estoqueMinimo) {
}
