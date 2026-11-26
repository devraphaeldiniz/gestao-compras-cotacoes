package com.compras.cotacoes.cotacao.dto;

import com.compras.cotacoes.cotacao.OrigemCotacao;
import com.compras.cotacoes.cotacao.StatusCotacao;
import com.compras.cotacoes.fornecedor.dto.FornecedorResponse;
import com.compras.cotacoes.proposta.CriterioDecisao;
import com.compras.cotacoes.proposta.dto.PropostaResponse;
import com.compras.cotacoes.proposta.dto.RecomendacaoResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CotacaoDetalheResponse(
        Long id,
        String codigo,
        OrigemCotacao origem,
        StatusCotacao status,
        List<CotacaoItemResponse> itens,
        LocalDate dataLimite,
        String observacao,
        Long propostaVencedoraId,
        boolean abertaAoMercado,
        List<FornecedorResponse> fornecedoresConvidados,
        List<PropostaResponse> propostas,
        int propostasElegiveis,
        int minimoPropostas,
        boolean podeDecidir,
        RecomendacaoResponse recomendacaoMenorPreco,
        RecomendacaoResponse recomendacaoPontuacaoPonderada,
        String criadoPor,
        CriterioDecisao criterioDecisao,
        String decididoPor,
        LocalDateTime decididoEm,
        LocalDateTime prazoEncerradoEm,
        String motivoCancelamento,
        String canceladoPor,
        LocalDateTime canceladoEm,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm) {
}
