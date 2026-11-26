package com.compras.cotacoes.cotacao.dto;

import com.compras.cotacoes.cotacao.OrigemCotacao;
import com.compras.cotacoes.cotacao.StatusCotacao;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CotacaoResumoResponse(
        Long id,
        String codigo,
        OrigemCotacao origem,
        StatusCotacao status,
        List<CotacaoItemResponse> itens,
        LocalDate dataLimite,
        long quantidadePropostas,
        Long propostaVencedoraId,
        String criadoPor,
        LocalDateTime criadoEm) {
}
