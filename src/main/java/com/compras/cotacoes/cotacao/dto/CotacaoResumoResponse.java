package com.compras.cotacoes.cotacao.dto;

import com.compras.cotacoes.cotacao.StatusCotacao;
import com.compras.cotacoes.produto.UnidadeMedida;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CotacaoResumoResponse(
        Long id,
        String codigo,
        Long produtoId,
        String produtoSku,
        String produtoNome,
        UnidadeMedida unidade,
        BigDecimal quantidade,
        StatusCotacao status,
        LocalDate dataLimite,
        String observacao,
        long quantidadePropostas,
        Long propostaVencedoraId,
        LocalDateTime criadoEm) {
}
