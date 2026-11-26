package com.compras.cotacoes.historico;

/** O texto exibido de cada ação vem de messages*.properties (chave {@code historico.<ACAO>}), no idioma de quem lê. */
public enum AcaoHistorico {
    PRODUTO_CRIADO,
    PRODUTO_ATUALIZADO,
    PRODUTO_ATIVADO,
    PRODUTO_INATIVADO,
    FORNECEDOR_CRIADO,
    FORNECEDOR_ATUALIZADO,
    FORNECEDOR_ATIVADO,
    FORNECEDOR_INATIVADO,
    COTACAO_CRIADA,
    COTACAO_ITEM_ADICIONADO,
    COTACAO_ABERTA,
    PROPOSTA_REGISTRADA,
    COTACAO_DECIDIDA,
    COTACAO_CANCELADA,
    PRAZO_ENCERRADO,
    CONVITE_ENVIADO,
    LEMBRETE_ENVIADO,
    PEDIDO_EMITIDO,
    PEDIDO_RECEBIMENTO,
    PEDIDO_CANCELADO
}
