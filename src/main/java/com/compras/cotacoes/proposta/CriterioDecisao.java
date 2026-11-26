package com.compras.cotacoes.proposta;

/**
 * MENOR_PRECO: menor valor total (desempate: menor prazo, depois maior validade).
 * PONTUACAO_PONDERADA: 70% preço + 30% prazo, numa escala de 0 a 100.
 */
public enum CriterioDecisao {
    MENOR_PRECO, PONTUACAO_PONDERADA
}
