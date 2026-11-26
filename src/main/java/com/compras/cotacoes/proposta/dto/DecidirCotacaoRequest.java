package com.compras.cotacoes.proposta.dto;

import com.compras.cotacoes.proposta.CriterioDecisao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record DecidirCotacaoRequest(
        @Schema(description = "Critério da recomendação; padrão MENOR_PRECO", example = "MENOR_PRECO")
        CriterioDecisao criterio,

        @Schema(description = "Opcional: proposta escolhida pelo comprador. Ausente = aceita a recomendada")
        Long propostaIdEscolhida,

        @Schema(description = "Obrigatória quando a proposta escolhida difere da recomendada")
        @Size(max = 500, message = "{validacao.tamanho.maximo}")
        String justificativaEscolha) {
}
