package com.compras.cotacoes.proposta;

/**
 * PENDENTE até a decisão. Depois: VENCEDORA, REJEITADA (era elegível e perdeu)
 * ou DESQUALIFICADA (vencida ou de fornecedor inativo no momento da decisão).
 */
public enum StatusProposta {
    PENDENTE, VENCEDORA, REJEITADA, DESQUALIFICADA
}
