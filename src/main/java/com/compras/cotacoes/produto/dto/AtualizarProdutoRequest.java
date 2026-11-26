package com.compras.cotacoes.produto.dto;

import com.compras.cotacoes.produto.UnidadeMedida;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** SKU e saldo não mudam por aqui: o SKU é a identidade do produto e o saldo só muda por movimentação. */
public record AtualizarProdutoRequest(
        @Schema(example = "Caneta esferográfica azul - caixa com 50")
        @NotBlank(message = "{validacao.obrigatorio}")
        @Size(max = 150, message = "{validacao.tamanho.maximo}")
        String nome,

        @NotNull(message = "{validacao.obrigatorio}")
        UnidadeMedida unidade,

        @Schema(example = "15")
        @NotNull(message = "{validacao.obrigatorio}")
        @PositiveOrZero(message = "{validacao.nao.negativo}")
        @Digits(integer = 15, fraction = 4, message = "{validacao.digitos}")
        BigDecimal estoqueMinimo) {
}
