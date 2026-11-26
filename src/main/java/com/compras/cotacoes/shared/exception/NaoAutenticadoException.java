package com.compras.cotacoes.shared.exception;

/** Credenciais recusadas no login. HTTP 401, sempre com a mesma mensagem (não revela se o usuário existe). */
public class NaoAutenticadoException extends ExcecaoLocalizada {

    public NaoAutenticadoException(String chave, Object... argumentos) {
        super(chave, argumentos);
    }
}
