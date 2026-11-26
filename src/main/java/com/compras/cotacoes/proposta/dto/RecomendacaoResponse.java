package com.compras.cotacoes.proposta.dto;

import com.compras.cotacoes.proposta.CriterioDecisao;
import java.util.List;

/** Prévia do critério: proposta recomendada e ranking completo das elegíveis. */
public record RecomendacaoResponse(
        CriterioDecisao criterio,
        Long propostaRecomendadaId,
        String fornecedorRecomendado,
        List<ItemRankingResponse> ranking) {
}
