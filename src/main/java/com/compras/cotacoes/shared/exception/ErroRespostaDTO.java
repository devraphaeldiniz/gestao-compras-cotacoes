package com.compras.cotacoes.shared.exception;

import java.time.OffsetDateTime;
import java.util.List;

/** Corpo padrão de erro da API. {@code campos} só vem preenchido em erros de validação (400). */
public record ErroRespostaDTO(
        OffsetDateTime timestamp,
        int status,
        String erro,
        String mensagem,
        String caminho,
        List<CampoInvalido> campos) {

    public record CampoInvalido(String campo, String mensagem) {
    }
}
