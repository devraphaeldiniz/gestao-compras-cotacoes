package com.compras.cotacoes.shared.exception;

/** Regra de negócio violada. Resposta HTTP 422 Unprocessable Entity. */
public class NegocioException extends ExcecaoLocalizada {

    public NegocioException(String chave, Object... argumentos) {
        super(chave, argumentos);
    }
}
