package com.compras.cotacoes.produto.dto;

import com.compras.cotacoes.produto.TipoMovimentacaoEstoque;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record MovimentarEstoqueRequest(
        @Schema(example = "SAIDA")
        @NotNull(message = "{validacao.obrigatorio}")
        TipoMovimentacaoEstoque tipo,

        @Schema(description = "Quantidade movimentada; no AJUSTE é o novo saldo absoluto", example = "5")
        @NotNull(message = "{validacao.obrigatorio}")
        @PositiveOrZero(message = "{validacao.nao.negativo}")
        BigDecimal quantidade) {
}
