package com.compras.cotacoes.proposta.dto;

import java.math.BigDecimal;

public record ItemRankingResponse(
        int posicao,
        Long propostaId,
        Long fornecedorId,
        String fornecedorRazaoSocial,
        BigDecimal valorTotal,
        Integer prazoEntregaDias,
        BigDecimal pontuacao) {
}
