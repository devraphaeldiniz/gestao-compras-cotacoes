package com.compras.cotacoes.proposta.dto;

import java.math.BigDecimal;

public record PropostaItemResponse(
        Long produtoId,
        String produtoSku,
        String produtoNome,
        BigDecimal quantidade,
        BigDecimal precoUnitario,
        BigDecimal valorMercadoria) {
}
