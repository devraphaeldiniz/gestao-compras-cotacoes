package com.compras.cotacoes.shared.exception;

/** Recurso inexistente. Resposta HTTP 404 Not Found. */
public class RecursoNaoEncontradoException extends ExcecaoLocalizada {

    public RecursoNaoEncontradoException(String chave, Object... argumentos) {
        super(chave, argumentos);
    }
}
