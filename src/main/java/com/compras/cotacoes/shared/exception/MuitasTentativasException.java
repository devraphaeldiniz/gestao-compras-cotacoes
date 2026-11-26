package com.compras.cotacoes.shared.exception;

import java.time.Duration;

/** Login bloqueado temporariamente por excesso de falhas. HTTP 429 com Retry-After. */
public class MuitasTentativasException extends ExcecaoLocalizada {

    private final Duration aguardar;

    public MuitasTentativasException(Duration aguardar) {
        super("auth.bloqueado", Math.max(1, (aguardar.toSeconds() + 59) / 60));
        this.aguardar = aguardar;
    }

    public Duration getAguardar() {
        return aguardar;
    }
}
