package com.compras.cotacoes.produto;

/**
 * ENTRADA soma ao saldo, SAIDA subtrai (sem permitir saldo negativo) e AJUSTE define o saldo absoluto após um
 * inventário: são os tipos que o usuário informa. RECEBIMENTO (pedido de compra) e SALDO_INICIAL (cadastro)
 * são gerados pelo sistema e só aparecem no extrato.
 */
public enum TipoMovimentacaoEstoque {
    SALDO_INICIAL, ENTRADA, SAIDA, AJUSTE, RECEBIMENTO;

    public boolean manual() {
        return this == ENTRADA || this == SAIDA || this == AJUSTE;
    }
}
