package com.compras.cotacoes.cotacao.dto;

import com.compras.cotacoes.produto.UnidadeMedida;
import java.math.BigDecimal;

public record CotacaoItemResponse(
        Long id,
        Long produtoId,
        String produtoSku,
        String produtoNome,
        UnidadeMedida unidade,
        BigDecimal quantidade,
        BigDecimal saldoAtual,
        BigDecimal estoqueMinimo) {
}
