package com.compras.cotacoes.produto;

/**
 * ENTRADA soma ao saldo, SAIDA subtrai (sem permitir saldo negativo)
 * e AJUSTE define o saldo absoluto após um inventário.
 */
public enum TipoMovimentacaoEstoque {
    ENTRADA, SAIDA, AJUSTE
}
