package com.compras.cotacoes.shared.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * Monta a página pedida pelo cliente com limites seguros. A ordenação é fixa por endpoint:
 * o cliente não escolhe campo de ordenação (evita ordenar por coluna sem índice ou inexistente).
 */
public final class Paginacao {

    public static final int TAMANHO_PADRAO = 20;
    public static final int TAMANHO_MAXIMO = 100;

    private Paginacao() {
    }

    public static PageRequest de(Integer pagina, Integer tamanho, Sort ordenacao) {
        int numero = pagina == null || pagina < 0 ? 0 : pagina;
        int linhas = tamanho == null || tamanho < 1 ? TAMANHO_PADRAO : Math.min(tamanho, TAMANHO_MAXIMO);
        return PageRequest.of(numero, linhas, ordenacao);
    }
}
