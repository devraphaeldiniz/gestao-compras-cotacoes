package com.compras.cotacoes.notificacao;

/** Publicado quando uma cotação é aberta; o convite aos fornecedores sai depois do commit. */
public record CotacaoAbertaEvento(Long cotacaoId) {
}
