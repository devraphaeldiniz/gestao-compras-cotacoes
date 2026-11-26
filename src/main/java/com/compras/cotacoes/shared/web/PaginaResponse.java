package com.compras.cotacoes.shared.web;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/** Envelope de listagens paginadas (contrato estável, independente do formato interno do Spring Data). */
public record PaginaResponse<T>(
        List<T> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas) {

    public static <E, T> PaginaResponse<T> de(Page<E> pagina, Function<E, T> conversor) {
        return new PaginaResponse<>(pagina.getContent().stream().map(conversor).toList(),
                pagina.getNumber(), pagina.getSize(), pagina.getTotalElements(), pagina.getTotalPages());
    }

    public static <T> PaginaResponse<T> de(Page<T> pagina) {
        return de(pagina, Function.identity());
    }
}
