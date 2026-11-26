package com.compras.cotacoes.shared.exception;

import java.util.Arrays;

/**
 * Exceção que carrega a chave da mensagem (messages*.properties) e seus argumentos.
 * O texto final é resolvido no idioma da requisição pelo {@link GlobalExceptionHandler}.
 */
public abstract class ExcecaoLocalizada extends RuntimeException {

    private final String chave;
    private final transient Object[] argumentos;

    protected ExcecaoLocalizada(String chave, Object... argumentos) {
        super(chave + (argumentos.length > 0 ? " " + Arrays.toString(argumentos) : ""));
        this.chave = chave;
        this.argumentos = argumentos.clone();
    }

    public String getChave() {
        return chave;
    }

    public Object[] getArgumentos() {
        return argumentos.clone();
    }
}
