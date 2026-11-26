package com.compras.cotacoes.pedido.dto;

import com.compras.cotacoes.produto.UnidadeMedida;
import java.math.BigDecimal;

public record PedidoItemResponse(
        Long produtoId,
        String produtoSku,
        String produtoNome,
        UnidadeMedida unidade,
        BigDecimal quantidade,
        BigDecimal quantidadeRecebida,
        BigDecimal quantidadePendente,
        BigDecimal valorUnitario,
        BigDecimal custoUnitarioEfetivo) {
}
