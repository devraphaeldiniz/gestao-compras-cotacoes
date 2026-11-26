package com.compras.cotacoes.pedido;

/** EMITIDO -> PARCIALMENTE_RECEBIDO -> RECEBIDO, ou CANCELADO (o saldo ainda não recebido é cancelado). */
public enum StatusPedidoCompra {
    EMITIDO, PARCIALMENTE_RECEBIDO, RECEBIDO, CANCELADO
}
