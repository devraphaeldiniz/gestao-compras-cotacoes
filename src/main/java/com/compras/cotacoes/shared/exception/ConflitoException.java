package com.compras.cotacoes.shared.exception;

/** Duplicidade detectada antes de chegar ao banco (SKU, documento, proposta repetida). HTTP 409 Conflict. */
public class ConflitoException extends ExcecaoLocalizada {

    public ConflitoException(String chave, Object... argumentos) {
        super(chave, argumentos);
    }
}
