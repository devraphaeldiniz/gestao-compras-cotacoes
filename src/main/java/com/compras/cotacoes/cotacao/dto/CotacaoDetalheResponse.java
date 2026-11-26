package com.compras.cotacoes.cotacao.dto;

import com.compras.cotacoes.cotacao.StatusCotacao;
import com.compras.cotacoes.fornecedor.dto.FornecedorResponse;
import com.compras.cotacoes.produto.UnidadeMedida;
import com.compras.cotacoes.proposta.dto.PropostaResponse;
import com.compras.cotacoes.proposta.dto.RecomendacaoResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CotacaoDetalheResponse(
        Long id,
        String codigo,
        Long produtoId,
        String produtoSku,
        String produtoNome,
        UnidadeMedida unidade,
        BigDecimal saldoAtualProduto,
        BigDecimal estoqueMinimoProduto,
        BigDecimal quantidade,
        StatusCotacao status,
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
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm) {
}
