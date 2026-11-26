package com.compras.cotacoes.proposta.dto;

import com.compras.cotacoes.cotacao.StatusCotacao;
import com.compras.cotacoes.pedido.dto.PedidoCompraResponse;
import com.compras.cotacoes.proposta.CriterioDecisao;
import java.util.List;

public record ResultadoDecisaoResponse(
        Long cotacaoId,
        String cotacaoCodigo,
        StatusCotacao statusCotacao,
        CriterioDecisao criterio,
        Long propostaRecomendadaId,
        boolean escolhaManual,
        String justificativaEscolha,
        PropostaResponse propostaVencedora,
        List<ItemRankingResponse> ranking,
        PedidoCompraResponse pedidoCompra) {
}
