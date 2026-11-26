package com.compras.cotacoes.produto.dto;

import com.compras.cotacoes.produto.TipoMovimentacaoEstoque;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record MovimentarEstoqueRequest(
        @Schema(description = "ENTRADA, SAIDA ou AJUSTE", example = "SAIDA")
        @NotNull(message = "{validacao.obrigatorio}")
        TipoMovimentacaoEstoque tipo,

        @Schema(description = "Quantidade movimentada; no AJUSTE é o novo saldo absoluto", example = "5")
        @NotNull(message = "{validacao.obrigatorio}")
        @PositiveOrZero(message = "{validacao.nao.negativo}")
        @Digits(integer = 15, fraction = 4, message = "{validacao.digitos}")
        BigDecimal quantidade,

        @Schema(description = "Motivo ou documento de referência (aparece no extrato)", example = "Requisição 1234 - TI")
        @Size(max = 500, message = "{validacao.tamanho.maximo}")
        String observacao) {

    public MovimentarEstoqueRequest(TipoMovimentacaoEstoque tipo, BigDecimal quantidade) {
        this(tipo, quantidade, null);
    }
}
